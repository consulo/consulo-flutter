/*
 * Copyright 2025 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package io.flutter;

import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.startup.PostStartupActivity;
import consulo.ui.UIAccess;
import io.flutter.logging.PluginLogger;
import org.jetbrains.annotations.NotNull;

// Consulo: upstream ProjectActivity (Kotlin, suspend) -> PostStartupActivity
public abstract class FlutterProjectActivity implements PostStartupActivity {

  private final Logger log = PluginLogger.createLogger(getClass());

  public abstract void executeProjectStartup(@NotNull Project project);

  @Override
  public void runActivity(@NotNull Project project, @NotNull UIAccess uiAccess) {
    try {
      executeProjectStartup(project);
    }
    catch (Throwable t) {
      log.error(t);
    }
  }

  @NotNull
  protected Logger getLog() {
    return log;
  }

  @NotNull
  protected Logger log() {
    return log;
  }
}
