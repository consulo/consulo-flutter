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
import org.jetbrains.annotations.NotNull;

@ActionImpl(id = "flutter.submitFeedback")
public class FlutterSubmitFeedback extends DumbAwareAction {
  public FlutterSubmitFeedback() {
    // Consulo: text, description and icon of upstream plugin.xml <action>
    getTemplatePresentation().setText(FlutterConsuloLocalize.actionFlutterSubmitfeedbackText());
    getTemplatePresentation().setDescription(FlutterConsuloLocalize.actionFlutterSubmitfeedbackDescription());
  }

  @Override
  public void actionPerformed(@NotNull final AnActionEvent e) {
    
    final String url = "https://github.com/flutter/flutter-intellij/issues/new";
    BrowserLauncher.getInstance().browse(url, null);
  }
}
