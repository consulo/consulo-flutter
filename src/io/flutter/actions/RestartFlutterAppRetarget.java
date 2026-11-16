/*
 * Copyright 2016 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.actions;

import consulo.annotation.component.ActionImpl;
import io.flutter.icons.FlutterIcons;

import consulo.ui.ex.action.ActionPlaces;

/**
 * A keystroke or toolbar invoked {@link RestartFlutterApp} action.
 */
@ActionImpl(id = "Flutter.Toolbar.RestartAction")
public class RestartFlutterAppRetarget extends FlutterRetargetAppAction {
  public RestartFlutterAppRetarget() {
    super(RestartFlutterApp.ID,
          RestartFlutterApp.TEXT,
          RestartFlutterApp.DESCRIPTION,
          ActionPlaces.MAIN_TOOLBAR,
          ActionPlaces.NAVIGATION_BAR_TOOLBAR,
          ActionPlaces.MAIN_MENU);
    // Consulo: icon of upstream plugin.xml <action>
    getTemplatePresentation().setIcon(FlutterIcons.HotRestart);
  }
}
