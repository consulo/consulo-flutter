/*
 * Copyright 2018 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.actions;

import consulo.annotation.component.ActionImpl;
import consulo.flutter.localize.FlutterConsuloLocalize;

import consulo.dataContext.DataContext;
import consulo.project.Project;
import io.flutter.FlutterMessages;
import io.flutter.pub.PubRoot;
import io.flutter.sdk.FlutterSdk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ActionImpl(id = "flutter.clean")
public class FlutterCleanAction extends FlutterSdkAction {
  public FlutterCleanAction() {
    // Consulo: text, description and icon of upstream plugin.xml <action>
    getTemplatePresentation().setText(FlutterConsuloLocalize.actionFlutterCleanText());
    getTemplatePresentation().setDescription(FlutterConsuloLocalize.actionFlutterCleanDescription());
  }

  @Override
  public void startCommand(@NotNull Project project, @NotNull FlutterSdk sdk, @Nullable PubRoot root, @NotNull DataContext context) {
    if (root == null) {
      FlutterMessages.showError(
        "Cannot Find Pub Root",
        "Flutter clean can only be run within a directory with a pubspec.yaml file",
        project);
      return;
    }

    sdk.flutterClean(root).startInConsole(project);
  }
}
