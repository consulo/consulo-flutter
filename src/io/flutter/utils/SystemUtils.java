/*
 * Copyright 2017 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.utils;

import consulo.process.ExecutionException;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.PathEnvironmentVariableUtil;
import consulo.process.util.ProcessOutput;
import consulo.process.util.CapturingProcessUtil;
import consulo.platform.Platform;
import consulo.application.util.concurrent.AppExecutorUtil;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.concurrent.CompletableFuture;

public class SystemUtils {
  
  // Replaces slated-for-removal `com.intellij.openapi.util.SystemInfo.getOsNameAndVersion()`.
  public static String getOsNameAndVersion() {
    // Consulo: Platform.current().os() instead of com.intellij.util.system.OS
    return Platform.current().os().name() + ' ' + Platform.current().os().version();
  }

  /**
   * Locate a given command-line tool given its name.
   * <p>
   * This is used to locate binaries that are not pre-installed. If it is necessary to find pre-installed
   * binaries it will require more work, especially on Windows.
   */
  @Nullable
  public static String which(String toolName) {
    final File gitExecutableFromPath =
      // Consulo: findInPath(name) always searches the PATH variable
      PathEnvironmentVariableUtil.findInPath(Platform.current().os().isWindows() ? toolName + ".exe" : toolName);
    if (gitExecutableFromPath != null) {
      return gitExecutableFromPath.getAbsolutePath();
    }
    return null;
  }

  /**
   * Execute the given command line, and return the process output as one result in a future.
   * <p>
   * This is a non-blocking equivalient to {@link CapturingProcessUtil#execAndGetOutput(GeneralCommandLine)}.
   */
  public static CompletableFuture<ProcessOutput> execAndGetOutput(GeneralCommandLine cmd) {
    final CompletableFuture<ProcessOutput> future = new CompletableFuture<>();

    AppExecutorUtil.getAppExecutorService().submit(() -> {
      try {
        final ProcessOutput output = CapturingProcessUtil.execAndGetOutput(cmd);
        future.complete(output);
      }
      catch (ExecutionException e) {
        future.completeExceptionally(e);
      }
    });

    return future;
  }
}
