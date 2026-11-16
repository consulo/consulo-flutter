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

@ActionImpl(id = "Flutter.Menu.RunProfileAction")
public class RunProfileFlutterApp extends RunFlutterAction {
  private static final String TEXT_DETAIL_MSG_KEY = "app.profile.config.action.text";

  public RunProfileFlutterApp() {
    super(TEXT_DETAIL_MSG_KEY, FlutterLaunchMode.PROFILE, ToolWindowId.RUN);
    // Consulo: text, description and icon of upstream plugin.xml <action>
    getTemplatePresentation().setText(FlutterConsuloLocalize.actionFlutterMenuRunprofileactionText());
    getTemplatePresentation().setDescription(FlutterConsuloLocalize.actionFlutterMenuRunprofileactionDescription());
    getTemplatePresentation().setIcon(PlatformIconGroup.actionsExecute());
  }
}
