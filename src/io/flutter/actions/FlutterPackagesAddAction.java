/*
 * Copyright 2025 The Chromium Authors. All rights reserved.
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

@ActionImpl(id = "flutter.pub.add")
public class FlutterPackagesAddAction extends FlutterSdkAction {
  public FlutterPackagesAddAction() {
    // Consulo: text, description and icon of upstream plugin.xml <action>
    getTemplatePresentation().setText(FlutterConsuloLocalize.actionFlutterPubAddText());
    getTemplatePresentation().setDescription(FlutterConsuloLocalize.actionFlutterPubAddDescription());
  }

  @Override
  public void startCommand(@NotNull Project project, @NotNull FlutterSdk sdk, @Nullable PubRoot root, @NotNull DataContext context) {
    if (root == null) {
      FlutterMessages.showError(
        "Cannot Find Pub Root",
        "Flutter pub add can only be run within a directory with a pubspec.yaml file",
        project);
      return;
    }
    PackageDialogWrapper dialog  = new PackageDialogWrapper();
    if (dialog.showAndGet()) {
      sdk.startPubAdd(root, project, dialog.getPackageName(), dialog.getDevOnly());
    }
  }
}

