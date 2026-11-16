/*
 * Copyright 2024 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package io.flutter.font;

import consulo.annotation.component.ExtensionImpl;

import consulo.application.ApplicationManager;
import consulo.project.Project;
import consulo.project.startup.BackgroundStartupActivity;
import consulo.ui.UIAccess;
import org.jetbrains.annotations.NotNull;

/**
 * This class functionality was previous provided by ProjectOpenListener.java, as part of
 * https://github.com/flutter/flutter-intellij/issues/6953, the functionality was moved over
 * as a ProjectActivity and backgroundPostStartupActivity extension.
 */
// Consulo: upstream ProjectActivity (Kotlin, backgroundPostStartupActivity) -> BackgroundStartupActivity
// Consulo: upstream order="last" is reserved for platform extensions
@ExtensionImpl(id = "FontPreviewStartupActivity")
public class FontPreviewStartupActivity implements BackgroundStartupActivity {
  @Override
  public void runActivity(@NotNull Project project, @NotNull UIAccess uiAccess) {
    // Ensure this isn't part of testing
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      return;
    }
    FontPreviewProcessor.analyze(project);
  }
}
