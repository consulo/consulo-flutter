/*
 * Copyright 2017 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.actions;

import consulo.annotation.component.ActionImpl;
import consulo.flutter.localize.FlutterConsuloLocalize;
import consulo.platform.base.icon.PlatformIconGroup;

import consulo.project.ui.wm.ToolWindowId;
import io.flutter.run.FlutterLaunchMode;

@ActionImpl(id = "Flutter.Menu.RunReleaseAction")
public class RunReleaseFlutterApp extends RunFlutterAction {
  private static final String TEXT_DETAIL_MSG_KEY = "app.release.config.action.text";

  public RunReleaseFlutterApp() {
    super(TEXT_DETAIL_MSG_KEY, FlutterLaunchMode.RELEASE, ToolWindowId.RUN);
    // Consulo: text, description and icon of upstream plugin.xml <action>
    getTemplatePresentation().setText(FlutterConsuloLocalize.actionFlutterMenuRunreleaseactionText());
    getTemplatePresentation().setDescription(FlutterConsuloLocalize.actionFlutterMenuRunreleaseactionDescription());
    getTemplatePresentation().setIcon(PlatformIconGroup.actionsExecute());
  }
}
