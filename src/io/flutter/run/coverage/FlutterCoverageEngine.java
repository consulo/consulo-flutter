/*
 * Copyright 2021 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.run.coverage;

import consulo.annotation.component.ExtensionImpl;

import consulo.application.Application;
import consulo.execution.coverage.CoverageAnnotator;
import consulo.execution.coverage.CoverageEngine;
import consulo.execution.coverage.CoverageFileProvider;
import consulo.execution.coverage.CoverageRunner;
import consulo.execution.coverage.CoverageSuite;
import consulo.execution.coverage.CoverageSuitesBundle;
import consulo.execution.test.AbstractTestProxy;
import consulo.language.psi.PsiElement;
import consulo.execution.configuration.RunConfigurationBase;
import consulo.execution.configuration.RunProfile;
import consulo.execution.configuration.WrappingRunConfiguration;
import consulo.execution.coverage.CoverageEnabledConfiguration;
import consulo.module.Module;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiFile;
import com.jetbrains.lang.dart.DartFileType;
import com.jetbrains.lang.dart.psi.DartFile;
import io.flutter.FlutterBundle;
import io.flutter.FlutterUtils;
import io.flutter.pub.PubRoot;
import io.flutter.run.test.TestConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ExtensionImpl
public class FlutterCoverageEngine extends CoverageEngine {

  public static FlutterCoverageEngine getInstance() {
    return Application.get().getExtensionPoint(CoverageEngine.class).findExtensionOrFail(FlutterCoverageEngine.class);
  }

  @Override
  public boolean isApplicableTo(@NotNull RunConfigurationBase conf) {
    return unwrapRunProfile(conf) instanceof TestConfig;
  }

  @Override
  public boolean canHavePerTestCoverage(@NotNull RunConfigurationBase conf) {
    return true;
  }

  @Override
  public @NotNull CoverageEnabledConfiguration createCoverageEnabledConfiguration(@NotNull RunConfigurationBase conf) {
    return new FlutterCoverageEnabledConfiguration(conf);
  }

  @Override
  public @Nullable CoverageSuite createCoverageSuite(@NotNull CoverageRunner covRunner,
                                                     @NotNull String name,
                                                     @NotNull CoverageFileProvider coverageDataFileProvider,
                                                     @Nullable String[] filters,
                                                     long lastCoverageTimeStamp,
                                                     @Nullable String suiteToMerge,
                                                     boolean coverageByTestEnabled,
                                                     boolean tracingEnabled,
                                                     boolean trackTestFolders,
                                                     Project project) {
    return null;
  }

  @Override
  public @Nullable CoverageSuite createCoverageSuite(@NotNull CoverageRunner covRunner,
                                                     @NotNull String name,
                                                     @NotNull CoverageFileProvider coverageDataFileProvider,
                                                     @NotNull CoverageEnabledConfiguration config) {
    if (config instanceof FlutterCoverageEnabledConfiguration) {
      return new FlutterCoverageSuite(covRunner, name, coverageDataFileProvider,
                                      config.getConfiguration().getProject(), this);
    }
    return null;
  }

  @Override
  public @Nullable CoverageSuite createEmptyCoverageSuite(@NotNull CoverageRunner coverageRunner) {
    return new FlutterCoverageSuite(this);
  }

  @Override
  public @NotNull CoverageAnnotator getCoverageAnnotator(Project project) {
    return FlutterCoverageAnnotator.getInstance(project);
  }

  @Override
  public boolean coverageEditorHighlightingApplicableTo(@NotNull PsiFile psiFile) {
    final PubRoot root = PubRoot.forPsiFile(psiFile);
    if (root == null) return false;
    final VirtualFile file = psiFile.getVirtualFile();
    if (file == null) return false;
    final String path = root.getRelativePath(file);
    if (path == null) return false;
    return path.startsWith("lib") && FlutterUtils.isDartFile(file);
  }

  @Override
  public boolean coverageProjectViewStatisticsApplicableTo(VirtualFile fileOrDir) {
    return !fileOrDir.isDirectory() && fileOrDir.getFileType() instanceof DartFileType;
  }

  @Override
  public boolean acceptedByFilters(@NotNull PsiFile psiFile, @NotNull CoverageSuitesBundle suite) {
    return psiFile instanceof DartFile;
  }

  @Override
  public boolean recompileProjectAndRerunAction(@NotNull Module module,
                                                @NotNull CoverageSuitesBundle suite,
                                                @NotNull Runnable chooseSuiteAction) {
    return false;
  }

  @Override
  public String getQualifiedName(@NotNull final File outputFile,
                                 @NotNull final PsiFile sourceFile) {
    return getQName(sourceFile);
  }

  @Override
  public @NotNull Set<String> getQualifiedNames(@NotNull PsiFile sourceFile) {
    final Set<String> qualifiedNames = new HashSet<>();
    qualifiedNames.add(getQName(sourceFile));
    return qualifiedNames;
  }

  // Consulo: abstract in CoverageEngine (IntelliJ has defaults); there is no per test coverage for Flutter
  @Override
  public @NotNull List<PsiElement> findTestsByNames(@NotNull String[] testNames, @NotNull Project project) {
    return List.of();
  }

  @Override
  public @Nullable String getTestMethodName(@NotNull PsiElement element, @NotNull AbstractTestProxy testProxy) {
    return null;
  }

  @Override
  public String getPresentableText() {
    return FlutterBundle.message("flutter.coverage.presentable.text");
  }

  @NotNull
  private static String getQName(@NotNull PsiFile sourceFile) {
    return sourceFile.getVirtualFile().getPath();
  }

  static @NotNull RunProfile unwrapRunProfile(@NotNull RunProfile runProfile) {
    if (runProfile instanceof WrappingRunConfiguration) {
      return ((WrappingRunConfiguration<?>)runProfile).getPeer();
    }
    return runProfile;
  }
}