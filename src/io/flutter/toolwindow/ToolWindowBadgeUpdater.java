/*
 * Copyright 2025 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.toolwindow;

import consulo.application.AllIcons;
import consulo.project.Project;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import io.flutter.run.common.RunMode;
import io.flutter.run.daemon.FlutterApp;

import java.util.Objects;

public class ToolWindowBadgeUpdater {
  /**
   * Updates the tool window icons for RUN or DEBUG mode with a green badge.
   *
   * @param app     The FlutterApp instance running in a given mode.
   * @param project The current IntelliJ project context.
   */
  public static void updateBadgedIcon(FlutterApp app, Project project) {
    final ToolWindowManager manager = ToolWindowManager.getInstance(Objects.requireNonNull(project));
    final ToolWindow runToolWindow = manager.getToolWindow(ToolWindowId.RUN);
    final ToolWindow debugToolWindow = manager.getToolWindow(ToolWindowId.DEBUG);

    if (Objects.requireNonNull(app).getMode() == RunMode.RUN) {
      if (runToolWindow != null) {
        manager.invokeLater(() -> {
          // Consulo: Image and ImageEffects.layered instead of Icon and LayeredIcon
          Image baseIcon = AllIcons.Toolwindows.ToolWindowRun;
          Image iconWithBadge = ImageEffects.layered(baseIcon, AllIcons.Nodes.RunnableMark);
          runToolWindow.setIcon(iconWithBadge);
        });
      }
    }
    else if (app.getMode() == RunMode.DEBUG) {
      manager.invokeLater(() -> {
        // https://github.com/flutter/flutter-intellij/issues/8391
        if (debugToolWindow != null) {
          Image baseIcon = PlatformIconGroup.toolwindowsToolwindowdebugger();
          Image iconWithBadge = ImageEffects.layered(baseIcon, AllIcons.Nodes.RunnableMark);
          debugToolWindow.setIcon(iconWithBadge);
        }
      });
    }
  }
}
