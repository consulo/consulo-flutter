/*
 * Copyright 2016 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.actions;

import consulo.annotation.component.ActionImpl;
import consulo.flutter.localize.FlutterConsuloLocalize;

import consulo.webBrowser.BrowserLauncher;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import io.flutter.FlutterConstants;
import org.jetbrains.annotations.NotNull;

@ActionImpl(id = "flutter.gettingStarted")
public class FlutterGettingStartedAction extends DumbAwareAction {
  public FlutterGettingStartedAction() {
    // Consulo: text, description and icon of upstream plugin.xml <action>
    getTemplatePresentation().setText(FlutterConsuloLocalize.actionFlutterGettingstartedText());
    getTemplatePresentation().setDescription(FlutterConsuloLocalize.actionFlutterGettingstartedDescription());
  }

  @Override
  public void actionPerformed(@NotNull final AnActionEvent e) {

    BrowserLauncher.getInstance().browse(FlutterConstants.URL_GETTING_STARTED_IDE, null);
  }
}
