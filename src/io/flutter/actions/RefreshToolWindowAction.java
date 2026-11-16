/*
 * Copyright 2024 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.actions;

import consulo.annotation.component.ActionImpl;
import consulo.flutter.localize.FlutterConsuloLocalize;

import consulo.application.AllIcons;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.project.Project;
import io.flutter.FlutterBundle;
import io.flutter.FlutterUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@ActionImpl(id = "io.flutter.RefreshToolWindow")
public class RefreshToolWindowAction extends DumbAwareAction {
  private String toolWindowId;

  public RefreshToolWindowAction() {
    super(FlutterBundle.message("flutter.toolwindow.action.refresh"), null, AllIcons.Actions.Refresh);
    // Consulo: description of upstream plugin.xml <action>
    getTemplatePresentation().setDescription(FlutterConsuloLocalize.actionIoFlutterRefreshtoolwindowDescription());
  }

  public RefreshToolWindowAction(@NotNull String toolWindowId) {
    super(FlutterBundle.message("flutter.toolwindow.action.refresh"), null, AllIcons.Actions.Refresh);
    this.toolWindowId = toolWindowId;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    final Project project = event.getData(Project.KEY);
    if (project == null) {
      return;
    }

    Optional.ofNullable(
        FlutterUtils.embeddedBrowser(project))
      .ifPresent(embeddedBrowser -> embeddedBrowser.refresh(toolWindowId));
  }
}
