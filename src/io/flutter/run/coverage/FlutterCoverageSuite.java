/*
 * Copyright 2021 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.run.coverage;

import consulo.execution.coverage.BaseCoverageSuite;
import consulo.execution.coverage.CoverageEngine;
import consulo.execution.coverage.CoverageFileProvider;
import consulo.execution.coverage.CoverageRunner;
import consulo.project.Project;
import org.jetbrains.annotations.NotNull;

public class FlutterCoverageSuite extends BaseCoverageSuite {

  @NotNull final private FlutterCoverageEngine coverageEngine;

  public FlutterCoverageSuite(@NotNull FlutterCoverageEngine coverageEngine) {
    this.coverageEngine = coverageEngine;
  }

  public FlutterCoverageSuite(CoverageRunner runner,
                              String name,
                              CoverageFileProvider coverageDataFileProvider,
                              Project project,
                              @NotNull FlutterCoverageEngine coverageEngine
  ) {
    super(name, coverageDataFileProvider, System.currentTimeMillis(), false, false,
          false, runner, project);
    this.coverageEngine = coverageEngine;
  }

  @Override
  public @NotNull CoverageEngine getCoverageEngine() {
    return coverageEngine;
  }

  // Consulo: not a CoverageSuite method
  public void deleteCachedCoverageData() {
  }
}
