/*
 * Copyright 2017 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.run;

import com.google.common.collect.ImmutableMap;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.ide.impl.idea.ide.actions.SaveAllAction;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationService;
import consulo.flutter.notification.FlutterRunNotificationGroups;
import consulo.localize.LocalizeValue;
import consulo.project.ui.notification.NotificationType;
import consulo.disposer.Disposable;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.dataContext.DataContext;
import consulo.ui.ex.action.event.AnActionListener;
import consulo.application.ApplicationManager;
import consulo.ui.ModalityState;
import consulo.container.boot.ContainerPathManager;
import consulo.logging.Logger;
import consulo.document.Document;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.document.FileDocumentManager;
import consulo.document.event.FileDocumentManagerListener;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.project.Project;
import consulo.ui.ex.popup.Balloon;
import consulo.application.util.SystemInfo;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.component.messagebus.MessageBusConnection;
import consulo.ui.ex.awt.UIUtil;
import com.jetbrains.lang.dart.ide.errorTreeView.DartProblemsView;
import com.jetbrains.lang.dart.psi.DartFile;
import io.flutter.icons.FlutterIcons;
import io.flutter.FlutterConstants;
import io.flutter.FlutterUtils;
import io.flutter.actions.FlutterAppAction;
import io.flutter.actions.ProjectActions;
import io.flutter.actions.ReloadFlutterApp;
import io.flutter.logging.PluginLogger;
import io.flutter.run.common.RunMode;
import io.flutter.run.daemon.FlutterApp;
import io.flutter.settings.FlutterSettings;
import io.flutter.utils.FlutterModuleUtils;
import io.flutter.utils.OpenApiUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Handle the mechanics of performing a hot reload on file save.
 */
public class FlutterReloadManager implements Disposable {
  private static final @NotNull Logger LOG = PluginLogger.createLogger(FlutterReloadManager.class);

  private static final Map<String, NotificationGroup> toolWindowNotificationGroups = new HashMap<>();

  private static final Map<String, String> toolWindowIdsToNotificationGroupIds = ImmutableMap.of(
    ToolWindowId.DEBUG, "flutter-debug",
    ToolWindowId.RUN, "flutter-run",
    DartProblemsView.TOOLWINDOW_ID, "flutter-analysis"
  );

  private static @Nullable NotificationGroup getNotificationGroup(@NotNull String toolWindowId) {
    if (!toolWindowNotificationGroups.containsKey(toolWindowId)) {
      final String notificationGroupId = toolWindowIdsToNotificationGroupIds.get(toolWindowId);
      if (notificationGroupId == null) {
        LOG.error("Notification group ID could not be found for toolWindowId: " + toolWindowId);
        return null;
      }

      // Consulo: no NotificationGroupManager, the groups are FlutterRunNotificationGroups
      NotificationGroup group = FlutterRunNotificationGroups.get(notificationGroupId);
      toolWindowNotificationGroups.put(toolWindowId, group);
    }
    return toolWindowNotificationGroups.get(toolWindowId);
  }

  private final @NotNull Project myProject;

  private Notification lastNotification;

  /**
   * Initialize the reload manager for the given project.
   */
  public static void init(@NotNull Project project) {
    // Call getInstance() will init FlutterReloadManager for the given project.
    getInstance(project);
  }

  @Nullable
  public static FlutterReloadManager getInstance(@NotNull Project project) {
    return project.getInstance(FlutterReloadManager.class);
  }

  private FlutterReloadManager(@NotNull Project project) {
    this.myProject = project;

    final MessageBusConnection connection =
      ApplicationManager.getApplication().getMessageBus().connect(this);
    connection.subscribe(AnActionListener.class, new AnActionListener() {
      private @Nullable Project eventProject;
      private @Nullable Editor eventEditor;

      @Override
      public void beforeActionPerformed(@NotNull AnAction action, @NotNull DataContext dataContext, @NotNull AnActionEvent event) {
        // In case of hot-reload breakages, see: https://github.com/flutter/flutter-intellij/issues/6996
        if (!(action instanceof SaveAllAction)) {
          return;
        }

        // Save the current project value here. If we access later (in afterActionPerformed), we can get
        // exceptions if another event occurs before afterActionPerformed is called.
        try {
          eventProject = event.getData(Project.KEY);
          eventEditor = event.getDataContext().getData(Editor.KEY);
        }
        catch (Throwable t) {
          // A catch-all, so any exceptions don't bubble through to the users.
          FlutterUtils.warn(LOG, "Exception from FlutterReloadManager", t, true);
        }
      }

      @Override
      public void afterActionPerformed(@NotNull AnAction action, @NotNull DataContext dataContext, @NotNull AnActionEvent event) {
        // In case of hot-reload breakages, see: https://github.com/flutter/flutter-intellij/issues/6996
        if (!(action instanceof SaveAllAction)) {
          return;
        }

        // If this save all action is not for the current project, return.
        if (myProject != eventProject) {
          return;
        }

        try {
          handleSaveAllNotification(eventEditor);
        }
        catch (Throwable t) {
          FlutterUtils.warn(LOG, "Exception from hot reload on save", t, true);
        }
        finally {
          // Context: "Released EditorImpl held by lambda in FlutterReloadManager" (https://github.com/flutter/flutter-intellij/issues/7507)
          eventProject = null;
          eventEditor = null;
        }
      }
    });
    connection.subscribe(FileDocumentManagerListener.class, new FileDocumentManagerListener() {
      @Override
      public void beforeAllDocumentsSaving() {
        if (!FlutterSettings.getInstance().isReloadOnSave()) return;
        if (myProject.isDisposed()) return;
        if (!FlutterModuleUtils.hasFlutterModule(myProject)) return;
        // The "Save files if the IDE is idle ..." option runs whether there are any changes or not.
        boolean isModified = false;
        final FileEditorManager fileEditorManager = FileEditorManager.getInstance(myProject);
        if (fileEditorManager != null) {
          for (FileEditor fileEditor : fileEditorManager.getAllEditors()) {
            if (fileEditor != null && fileEditor.isModified()) {
              isModified = true;
              break;
            }
          }
          if (!isModified) return;
        }

        OpenApiUtils.safeInvokeLater(() -> {
          // Find a Dart editor to trigger the reload.
          final Editor anEditor = OpenApiUtils.safeRunReadAction(() -> {
            Editor someEditor = null;
            final EditorFactory editorFactory = EditorFactory.getInstance();
            if (editorFactory != null) {
              for (Editor editor : editorFactory.getAllEditors()) {
                if (editor == null || editor.isDisposed() || editor.getProject() != myProject) {
                  continue;
                }
                final PsiFile psiFile = PsiDocumentManager.getInstance(myProject).getPsiFile(editor.getDocument());
                if (psiFile instanceof DartFile && someEditor == null) {
                  someEditor = editor;
                }
                if (null != PsiTreeUtil.findChildOfType(psiFile, PsiErrorElement.class, false)) {
                  // The Dart plugin may create empty files that it then claims have a syntax error. Ignore them.
                  if (editor.getDocument().getTextLength() != 0) {
                    // If there are analysis errors we want to silently exit, without showing a notification.
                    return null;
                  }
                }
              }
            }
            return someEditor;
          });
          handleSaveAllNotification(anEditor);
        }, ModalityState.any());
      }
    });
  }

  private void handleSaveAllNotification(@Nullable Editor editor) {
    if (!FlutterSettings.getInstance().isReloadOnSave() || editor == null) {
      return;
    }

    @NotNull String configPath = ContainerPathManager.get().getConfigPath();
    if (SystemInfo.isWindows) {
      configPath = configPath.replace('\\', '/');
    }
    @Nullable VirtualFile file = FileDocumentManager.getInstance().getFile(editor.getDocument());
    if (file == null) {
      return;
    }
    if (file.getPath().startsWith(configPath)) {
      return; // Ignore changes to scratch files.
    }
    if (System.currentTimeMillis() - file.getTimeStamp() > 500) {
      // If the file was saved in the last half-second, assume it should trigger hot reload
      // because it was probably just saved before this notification was generated.
      // Files saved a long time ago should not trigger hot reload.
      return;
    }

    final AnAction reloadAction = ProjectActions.getAction(myProject, ReloadFlutterApp.ID);
    final FlutterApp app = getApp(reloadAction);
    if (app == null) {
      return;
    }

    if (!app.getLaunchMode().supportsReload() || !app.appSupportsHotReload()) {
      return;
    }

    if (!app.isStarted() || app.isReloading()) {
      return;
    }

    // Transition the app to an about-to-reload state.
    final FlutterApp.State previousAppState = app.transitionStartingHotReload();

    AppExecutorUtil.getAppScheduledExecutorService().schedule(() -> {
      clearLastNotification();

      if (!app.isConnected()) {
        return;
      }

      // Don't reload if we find structural errors with the current file.
      if (hasErrorsInFile(editor.getDocument())) {
        app.cancelHotReloadState(previousAppState);
        showAnalysisNotification("Reload not performed", "Analysis issues found", true);

        return;
      }

      final Notification notification = showRunNotification(app, null, "Reloading…", false);
      final long startTime = System.currentTimeMillis();

      app.performHotReload(true, FlutterConstants.RELOAD_REASON_SAVE).thenAccept(result -> {
        if (!result.ok()) {
          if (notification != null) {
            notification.expire();
          }
          showRunNotification(app, "Hot Reload Error", result.getMessage(), true);
        }
        else {
          // Make sure the reloading message is displayed for at least 2 seconds (so it doesn't just flash by).
          final long delay = Math.max(0, 2000 - (System.currentTimeMillis() - startTime));

          AppExecutorUtil.getAppScheduledExecutorService().schedule(() -> UIUtil.invokeLaterIfNeeded(() -> {
            if (notification != null) {
              notification.expire();
            }

            // If the 'Reloading…' notification is still the most recent one, then clear it.
            if (isLastNotification(notification)) {
              removeRunNotifications(app);
            }
          }), delay, TimeUnit.MILLISECONDS);
        }
      });
    }, 0, TimeUnit.MILLISECONDS);
  }

  private void reloadApp(@NotNull FlutterApp app, @NotNull String reason) {
    if (app.isStarted()) {
      app.performHotReload(true, reason).thenAccept(result -> {
        if (!result.ok()) {
          showRunNotification(app, "Hot Reload Error", result.getMessage(), true);
        }
      }).exceptionally(throwable -> {
        showRunNotification(app, "Hot Reload Error", throwable.getMessage(), true);
        return null;
      });
    }
  }

  public void saveAllAndReload(@NotNull FlutterApp app, @NotNull String reason) {
    FileDocumentManager.getInstance().saveAllDocuments();

    clearLastNotification();

    reloadApp(app, reason);
  }

  public void saveAllAndReloadAll(@NotNull List<FlutterApp> appsToReload, @NotNull String reason) {
    FileDocumentManager.getInstance().saveAllDocuments();

    clearLastNotification();

    for (FlutterApp app : appsToReload) {
      reloadApp(app, reason);
    }
  }

  private void restartApp(@NotNull FlutterApp app, @NotNull String reason) {
    if (app.isStarted()) {
      app.performRestartApp(reason).thenAccept(result -> {
        if (!result.ok()) {
          showRunNotification(app, "Hot Restart Error", result.getMessage(), true);
        }
      }).exceptionally(throwable -> {
        showRunNotification(app, "Hot Restart Error", throwable.getMessage(), true);
        return null;
      });

      final FlutterDevice device = app.device();
      device.bringToFront();
    }
  }

  public void saveAllAndRestart(@NotNull FlutterApp app, @NotNull String reason) {
    FileDocumentManager.getInstance().saveAllDocuments();

    clearLastNotification();

    restartApp(app, reason);
  }

  public void saveAllAndRestartAll(@NotNull List<FlutterApp> appsToRestart, @NotNull String reason) {
    FileDocumentManager.getInstance().saveAllDocuments();

    clearLastNotification();

    for (FlutterApp app : appsToRestart) {
      restartApp(app, reason);
    }
  }

  @Nullable
  private FlutterApp getApp(AnAction reloadAction) {
    if (reloadAction instanceof FlutterAppAction) {
      return ((FlutterAppAction)reloadAction).getApp();
    }
    else {
      return null;
    }
  }

  private void showAnalysisNotification(@NotNull String title, @NotNull String content, boolean isError) {
    final ToolWindow dartProblemsView = ToolWindowManager.getInstance(myProject).getToolWindow(DartProblemsView.TOOLWINDOW_ID);

    final NotificationGroup notificationGroup = getNotificationGroup(DartProblemsView.TOOLWINDOW_ID);
    if (notificationGroup == null) {
      LOG.error("Could not find notification group for toolWindowId: " + DartProblemsView.TOOLWINDOW_ID);
      return;
    }
    final Notification.Builder builder = NotificationService.getInstance()
      .newOfType(notificationGroup, isError ? NotificationType.ERROR : NotificationType.INFORMATION)
      .title(LocalizeValue.of(title))
      .content(LocalizeValue.of(content));

    if (dartProblemsView != null && !dartProblemsView.isVisible()) {
      builder.addClosingAction(LocalizeValue.localizeTODO("View Issues"), () -> {
        final ToolWindow dartProblemsView1 = ToolWindowManager.getInstance(myProject).getToolWindow(DartProblemsView.TOOLWINDOW_ID);
        if (dartProblemsView1 != null) {
          dartProblemsView1.activate(null);
        }
      });
    }

    builder.icon(FlutterIcons.Flutter);
    final Notification notification = builder.notifyAndGet(myProject);

    lastNotification = notification;
  }

  private @Nullable Notification showRunNotification(@NotNull FlutterApp app,
                                                     @Nullable String title,
                                                     @NotNull String content,
                                                     boolean isError) {
    final String toolWindowId = app.getMode() == RunMode.DEBUG ? ToolWindowId.DEBUG : ToolWindowId.RUN;
    final NotificationGroup notificationGroup = getNotificationGroup(toolWindowId);
    if (notificationGroup == null) {
      LOG.error("Could not find notification group for toolWindowId: " + DartProblemsView.TOOLWINDOW_ID);
      return null;
    }

    final Notification.Builder builder = NotificationService.getInstance()
      .newOfType(notificationGroup, isError ? NotificationType.ERROR : NotificationType.INFORMATION)
      .content(LocalizeValue.of(content))
      .icon(FlutterIcons.Flutter);
    final Notification notification = builder.notifyAndGet(myProject);

    lastNotification = notification;

    return notification;
  }

  private boolean isLastNotification(final Notification notification) {
    return notification == lastNotification;
  }

  private void clearLastNotification() {
    lastNotification = null;
  }

  private void removeRunNotifications(FlutterApp app) {
    final String toolWindowId = app.getMode() == RunMode.DEBUG ? ToolWindowId.DEBUG : ToolWindowId.RUN;
    // Consulo: no getToolWindowBalloon, the manager hides the balloon itself
    ToolWindowManager.getInstance(myProject).hideToolWindowBalloon(toolWindowId);
  }

  private boolean hasErrorsInFile(@NotNull Document document) {
    // We use the IntelliJ parser and look for syntax errors in the current document.
    // We block reload if we find issues in the immediate file. We don't block reload if there
    // are analysis issues in other files; the compilation errors from the flutter tool
    // will indicate to the user where the problems are.

    final PsiErrorElement firstError = OpenApiUtils.safeRunReadAction(() -> {
      final PsiFile psiFile = PsiDocumentManager.getInstance(myProject).getPsiFile(document);
      if (psiFile instanceof DartFile) {
        return PsiTreeUtil.findChildOfType(psiFile, PsiErrorElement.class, false);
      }
      else {
        return null;
      }
    });
    return firstError != null;
  }

  @Override
  public void dispose() {
  }
}