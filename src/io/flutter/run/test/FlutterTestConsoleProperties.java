/*
 * Copyright 2022 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.run.test;

import consulo.execution.executor.Executor;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.test.sm.runner.SMTRunnerConsoleProperties;
import org.jetbrains.annotations.NotNull;

/**
 * This is required to enable the auto-test-before-commit feature in the VCS tool window.
 */
public class FlutterTestConsoleProperties extends SMTRunnerConsoleProperties {

  public FlutterTestConsoleProperties(@NotNull RunConfiguration config, @NotNull Executor executor) {
    super(config, "FlutterWidgetTests", executor);
  }
}
