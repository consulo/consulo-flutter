/*
 * Copyright 2016 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.actions;

import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnActionEvent;
import consulo.project.Project;
import consulo.application.util.function.Computable;
import io.flutter.icons.FlutterIcons;
import io.flutter.FlutterBundle;
import io.flutter.FlutterConstants;
import io.flutter.run.FlutterReloadManager;
import io.flutter.run.daemon.FlutterApp;
import org.jetbrains.annotations.NotNull;

public class RestartFlutterApp extends FlutterAppAction {
  public static final String ID = "Flutter.RestartFlutterApp"; //NON-NLS
  public static final String TEXT = FlutterBundle.message("app.restart.action.text");
  public static final String DESCRIPTION = FlutterBundle.message("app.restart.action.description");

  public RestartFlutterApp(@NotNull FlutterApp app, @NotNull Computable<Boolean> isApplicable) {
    super(app, TEXT, DESCRIPTION, FlutterIcons.HotRestart, isApplicable, ID);
    // Shortcut is associated with toolbar action.
    ActionManager actionManager = ActionManager.getInstance();
    if (actionManager != null) {
      var action = actionManager.getAction("Flutter.Toolbar.RestartAction");
      if (action != null) {
        copyShortcutFrom(action);
      }
    }
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final Project project = e.getData(Project.KEY);
    if (project == null) {
      return;
    }
    var reloadManager = FlutterReloadManager.getInstance(project);
    if (reloadManager == null) return;

    reloadManager.saveAllAndRestart(getApp(), FlutterConstants.RELOAD_REASON_MANUAL);
  }
}
