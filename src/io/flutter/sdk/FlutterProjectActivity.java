/*
 * Copyright 2025 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package io.flutter.sdk;

import consulo.project.Project;
import consulo.project.startup.BackgroundStartupActivity;
import consulo.ui.UIAccess;
import org.jetbrains.annotations.NotNull;

// Consulo: upstream ProjectActivity (suspend, registered as backgroundPostStartupActivity) is a BackgroundStartupActivity
public class FlutterProjectActivity implements BackgroundStartupActivity {
  @Override
  public void runActivity(@NotNull Project project, @NotNull UIAccess uiAccess) {
    FlutterSdkManager.getInstance(project).checkForFlutterSdkChange();
  }
}
