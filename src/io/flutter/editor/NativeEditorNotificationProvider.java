/*
 * Copyright 2018 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.editor;

import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionUiKind;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.openapi.actionSystem.impl.SimpleDataContext;
import consulo.fileEditor.FileEditor;
import consulo.project.Project;
import consulo.project.util.ProjectUtil;
import consulo.application.util.SystemInfo;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.impl.idea.ui.EditorNotificationPanel;
import consulo.fileEditor.EditorNotificationProvider;
import consulo.fileEditor.EditorNotifications;
import io.flutter.icons.FlutterIcons;
import io.flutter.FlutterBundle;
import io.flutter.utils.UIUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.util.function.Function;

public class NativeEditorNotificationProvider implements EditorNotificationProvider {
  private final Project project;
  private boolean showNotification = true;

  public NativeEditorNotificationProvider(@NotNull Project project) {
    this.project = project;
  }

  @Override
  public @Nullable Function<? super @NotNull FileEditor, ? extends @Nullable JComponent> collectNotificationData(@NotNull Project project,
                                                                                                                 @NotNull VirtualFile file) {
    if (!file.isInLocalFileSystem() || !showNotification) {
      return null;
    }

    VirtualFile root = ProjectUtil.guessProjectDir(project);
    if (root == null) {
      return null;
    }
    return fileEditor -> createPanelForAction(fileEditor, root, getActionName(root));
  }

  @Nullable
  private EditorNotificationPanel createPanelForAction(@NotNull FileEditor fileEditor,
                                                       @NotNull VirtualFile root,
                                                       @Nullable String actionName) {
    if (actionName == null) {
      return null;
    }
    final NativeEditorActionsPanel panel = new NativeEditorActionsPanel(fileEditor, root, actionName);
    return panel.isValidForFile() ? panel : null;
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
    if (SystemInfo.isMac) {
      if (root.getName().equals("ios")) {
        return "flutter.xcode.open";
      }
      else if (root.getName().equals("macos")) {
        return "flutter.xcode.open";
      }
    }
    return null;
  }

  class NativeEditorActionsPanel extends EditorNotificationPanel {
    final VirtualFile myFile;
    final VirtualFile myRoot;
    final AnAction myAction;
    final boolean isVisible;

    NativeEditorActionsPanel(@NotNull FileEditor fileEditor, @NotNull VirtualFile root, @NotNull String actionName) {
      super(UIUtils.getEditorNotificationBackgroundColor());
      myFile = fileEditor.getFile();
      myRoot = root;
      myAction = ActionManager.getInstance().getAction(actionName);

      icon(FlutterIcons.Flutter);
      text("Flutter commands");

      // Ensure this project is a Flutter project by updating the menu action. It will only be visible for Flutter projects.
      myAction.update(
        AnActionEvent.createEvent(makeContext(), myAction.getTemplatePresentation(), ActionPlaces.EDITOR_TOOLBAR, ActionUiKind.NONE, null));

      isVisible = myAction.getTemplatePresentation().isVisible();
      //noinspection DialogTitleCapitalization
      createActionLabel(myAction.getTemplatePresentation().getText(), this::performAction)
        .setToolTipText(myAction.getTemplatePresentation().getDescription());
      createActionLabel(FlutterBundle.message("flutter.androidstudio.open.hide.notification.text"), () -> {
        showNotification = false;
        EditorNotifications.getInstance(project).updateAllNotifications();
      }).setToolTipText(FlutterBundle.message("flutter.androidstudio.open.hide.notification.description"));
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
        AnActionEvent.createEvent(makeContext(), myAction.getTemplatePresentation(), ActionPlaces.EDITOR_TOOLBAR, ActionUiKind.NONE, null));
    }

    private DataContext makeContext() {
      return SimpleDataContext.builder().add(CommonDataKeys.VIRTUAL_FILE, myFile).add(CommonDataKeys.PROJECT, project).build();
    }
  }
}
