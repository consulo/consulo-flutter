/*
 * Copyright 2018 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.editor;

import consulo.annotation.access.RequiredReadAction;
import consulo.dataContext.DataContext;
import consulo.fileEditor.EditorNotificationBuilder;
import consulo.fileEditor.EditorNotificationProvider;
import consulo.fileEditor.EditorNotifications;
import consulo.fileEditor.FileEditor;
import consulo.language.editor.CommonDataKeys;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.project.util.ProjectUtil;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.VirtualFile;
import io.flutter.FlutterBundle;
import io.flutter.icons.FlutterIcons;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class NativeEditorNotificationProvider implements EditorNotificationProvider {
  private final Project project;
  private boolean showNotification = true;

  @Inject
  public NativeEditorNotificationProvider(@NotNull Project project) {
    this.project = project;
  }

  @NotNull
  @Override
  public String getId() {
    return "flutter.native.editor.notification";
  }

  // Consulo: EditorNotificationProvider fills an EditorNotificationBuilder instead of returning a panel factory
  @RequiredReadAction
  @Nullable
  @Override
  public EditorNotificationBuilder buildNotification(@NotNull VirtualFile file,
                                                     @NotNull FileEditor fileEditor,
                                                     @NotNull Supplier<EditorNotificationBuilder> builderFactory) {
    if (!file.isInLocalFileSystem() || !showNotification) {
      return null;
    }

    VirtualFile root = ProjectUtil.guessProjectDir(project);
    if (root == null) {
      return null;
    }
    return createPanelForAction(file, root, getActionName(root), builderFactory);
  }

  @Nullable
  private EditorNotificationBuilder createPanelForAction(@NotNull VirtualFile file,
                                                         @NotNull VirtualFile root,
                                                         @Nullable String actionName,
                                                         @NotNull Supplier<EditorNotificationBuilder> builderFactory) {
    if (actionName == null) {
      return null;
    }
    final NativeEditorActionsPanel panel = new NativeEditorActionsPanel(file, root, actionName);
    return panel.isValidForFile() ? panel.fill(builderFactory.get()) : null;
  }

  @Nullable
  private static String getActionName(@Nullable VirtualFile root) {
    if (root == null) {
      return null;
    }

    // See https://github.com/flutter/flutter-intellij/issues/7103
    //if (root.getName().equals("android")) {
    //  return "flutter.androidstudio.open";
    //}
    //else
    if (Platform.current().os().isMac()) {
      if (root.getName().equals("ios")) {
        return "flutter.xcode.open";
      }
      else if (root.getName().equals("macos")) {
        return "flutter.xcode.open";
      }
    }
    return null;
  }

  class NativeEditorActionsPanel {
    final VirtualFile myFile;
    final VirtualFile myRoot;
    final AnAction myAction;
    final boolean isVisible;

    NativeEditorActionsPanel(@NotNull VirtualFile file, @NotNull VirtualFile root, @NotNull String actionName) {
      myFile = file;
      myRoot = root;
      myAction = ActionManager.getInstance().getAction(actionName);

      // Ensure this project is a Flutter project by updating the menu action. It will only be visible for Flutter projects.
      myAction.update(
        AnActionEvent.createFromDataContext(ActionPlaces.EDITOR_TOOLBAR, myAction.getTemplatePresentation(), makeContext()));

      isVisible = myAction.getTemplatePresentation().isVisible();
    }

    @NotNull
    EditorNotificationBuilder fill(@NotNull EditorNotificationBuilder builder) {
      builder.withIcon(FlutterIcons.Flutter);
      builder.withText(LocalizeValue.localizeTODO("Flutter commands"));

      //noinspection DialogTitleCapitalization
      builder.withAction(myAction.getTemplatePresentation().getTextValue(),
                         myAction.getTemplatePresentation().getDescription(),
                         e -> performAction());
      builder.withAction(FlutterBundle.messageValue("flutter.androidstudio.open.hide.notification.text"),
                         FlutterBundle.messageValue("flutter.androidstudio.open.hide.notification.description"),
                         e -> {
                           showNotification = false;
                           EditorNotifications.getInstance(project).updateAllNotifications();
                         });
      return builder;
    }

    private boolean isValidForFile() {
      if (isVisible) {
        // The menu items are visible for certain elements outside the module directories.
        return FileUtil.isAncestor(myRoot.getPath(), myFile.getPath(), true);
      }
      return false;
    }

    private void performAction() {
      // Open Xcode or Android Studio. If already running AS then just open a new window.
      myAction.actionPerformed(
        AnActionEvent.createFromDataContext(ActionPlaces.EDITOR_TOOLBAR, myAction.getTemplatePresentation(), makeContext()));
    }

    private DataContext makeContext() {
      return DataContext.builder().add(CommonDataKeys.VIRTUAL_FILE, myFile).add(CommonDataKeys.PROJECT, project).build();
    }
  }
}
