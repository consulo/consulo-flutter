/*
 * Copyright 2019 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.actions;

import consulo.annotation.component.ActionImpl;
import io.flutter.icons.FlutterIcons;

import consulo.ui.ex.action.ActionPlaces;

@ActionImpl(id = "Flutter.Toolbar.RestartAllAction")
public class RestartAllFlutterAppsRetarget extends FlutterRetargetAppAction {
  public RestartAllFlutterAppsRetarget() {
    super(RestartAllFlutterApps.ID,
          RestartAllFlutterApps.TEXT,
          RestartAllFlutterApps.DESCRIPTION,
          ActionPlaces.MAIN_TOOLBAR,
          ActionPlaces.NAVIGATION_BAR_TOOLBAR,
          ActionPlaces.MAIN_MENU);
    // Consulo: icon of upstream plugin.xml <action>
    getTemplatePresentation().setIcon(FlutterIcons.HotRestart);
  }
}
