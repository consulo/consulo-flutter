/*
 * Copyright 2016 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.actions;

import consulo.annotation.component.ActionImpl;
import consulo.flutter.localize.FlutterConsuloLocalize;

import consulo.dataContext.DataContext;
import consulo.project.Project;
import io.flutter.pub.PubRoot;
import io.flutter.sdk.FlutterSdk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ActionImpl(id = "flutter.upgrade")
public class FlutterUpgradeAction extends FlutterSdkAction {
  public FlutterUpgradeAction() {
    // Consulo: text, description and icon of upstream plugin.xml <action>
    getTemplatePresentation().setText(FlutterConsuloLocalize.actionFlutterUpgradeText());
    getTemplatePresentation().setDescription(FlutterConsuloLocalize.actionFlutterUpgradeDescription());
  }

  @Override
  public void startCommand(@NotNull Project project, @NotNull FlutterSdk sdk, @Nullable PubRoot root, @NotNull DataContext context) {
    sdk.flutterUpgrade().startInConsole(project);
  }
}
