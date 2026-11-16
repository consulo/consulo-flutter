/*
 * Copyright 2020 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.actions;

import consulo.annotation.component.ActionImpl;
import consulo.flutter.localize.FlutterConsuloLocalize;
import io.flutter.icons.FlutterIcons;

import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.project.Project;
import io.flutter.run.daemon.DeviceService;
import io.flutter.utils.FlutterModuleUtils;
import org.jetbrains.annotations.NotNull;

@ActionImpl(id = "Flutter.DeviceSelectorRefresher")
public class DeviceSelectorRefresherAction extends AnAction implements AnActionWithSyncUpdate {
  public DeviceSelectorRefresherAction() {
    // Consulo: text, description and icon of upstream plugin.xml <action>
    getTemplatePresentation().setText(FlutterConsuloLocalize.actionFlutterDeviceselectorrefresherText());
    getTemplatePresentation().setDescription(FlutterConsuloLocalize.actionFlutterDeviceselectorrefresherDescription());
    getTemplatePresentation().setIcon(FlutterIcons.RefreshItems);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final Project project = e.getData(Project.KEY);
    if (project == null) return;
    DeviceService.getInstance(project).restart();
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    var project = e.getData(Project.KEY);
    if (project != null) {
      e.getPresentation().setVisible(FlutterModuleUtils.hasInternalDartSdkPath(project));
    }
  }
}
