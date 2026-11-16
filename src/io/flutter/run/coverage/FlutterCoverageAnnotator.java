/*
 * Copyright 2021 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.run.coverage;

import consulo.execution.coverage.CoverageDataManager;
import consulo.execution.coverage.CoverageSuitesBundle;
import consulo.execution.coverage.SimpleCoverageAnnotator;
import consulo.module.Module;
import consulo.project.Project;
import consulo.module.content.ModuleRootManager;
import consulo.virtualFileSystem.VirtualFile;
import consulo.util.collection.SmartList;
import io.flutter.utils.FlutterModuleUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class FlutterCoverageAnnotator extends SimpleCoverageAnnotator {

  @Nullable
  public static FlutterCoverageAnnotator getInstance(Project project) {
    return project.getInstance(FlutterCoverageAnnotator.class);
  }

  public FlutterCoverageAnnotator(Project project) {
    super(project);
  }

  @Override
  protected FileCoverageInfo fillInfoForUncoveredFile(@NotNull File file) {
    return new FileCoverageInfo();
  }

  // Consulo: SimpleCoverageAnnotator has no shouldCollectCoverageInsideLibraryDirs/getRoots hooks; it annotates the
  // content roots of all modules. The upstream overrides are kept for reference.
  protected boolean shouldCollectCoverageInsideLibraryDirs() {
    return false;
  }

  protected VirtualFile[] getRoots(Project project,
                                   @NotNull CoverageDataManager dataManager,
                                   CoverageSuitesBundle suite) {
    return dataManager.doInReadActionIfProjectOpen(() -> {
      final List<VirtualFile> roots = new SmartList<>();
      for (Module module : FlutterModuleUtils.findModulesWithFlutterContents(project)) {
        final ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
        roots.addAll(Arrays.asList(rootManager.getContentRoots()));
      }
      return roots.toArray(VirtualFile.EMPTY_ARRAY);
    });
  }
}