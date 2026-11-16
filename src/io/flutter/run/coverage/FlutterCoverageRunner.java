/*
 * Copyright 2021 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.run.coverage;

import consulo.execution.coverage.CoverageEngine;
import consulo.execution.coverage.CoverageRunner;
import consulo.execution.coverage.CoverageSuite;
import consulo.logging.Logger;
import consulo.execution.coverage.data.CoverageProjectData;
import consulo.execution.coverage.data.CoverageProjectDataImpl;
import io.flutter.FlutterBundle;
import io.flutter.logging.PluginLogger;
import io.flutter.settings.FlutterSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

public class FlutterCoverageRunner extends CoverageRunner {
  private static final String ID = "FlutterCoverageRunner";
  private static final @NotNull Logger LOG = PluginLogger.createLogger(FlutterCoverageRunner.class);

  @Nullable
  @Override
  public CoverageProjectData loadCoverageData(@NotNull final File sessionDataFile, @Nullable CoverageSuite baseCoverageSuite) {
    if (!(baseCoverageSuite instanceof FlutterCoverageSuite)) {
      return null;
    }
    return doLoadCoverageData(sessionDataFile, (FlutterCoverageSuite)baseCoverageSuite);
  }

  @Nullable
  private static CoverageProjectData doLoadCoverageData(@NotNull final File sessionDataFile, @NotNull final FlutterCoverageSuite coverageSuite) {
    final CoverageProjectData projectData = new CoverageProjectDataImpl();
    try {
      LcovInfo.readInto(projectData, sessionDataFile);
    }
    catch (IOException ex) {
      LOG.warn(FlutterBundle.message("coverage.data.not.read", FlutterSettings.getInstance().isFilePathLoggingEnabled()
                                                               ? sessionDataFile.getAbsolutePath()
                                                               : sessionDataFile.getName()));
      return null;
    }
    return projectData;
  }

  @NotNull
  @Override
  public String getPresentableName() {
    return "Flutter";
  }

  @NotNull
  @Override
  public String getId() {
    return ID;
  }

  @NotNull
  @Override
  public String getDataFileExtension() {
    return "info";
  }

  @Override
  public boolean acceptsCoverageEngine(@NotNull CoverageEngine engine) {
    return engine instanceof FlutterCoverageEngine;
  }
}
