/*
 * Copyright 2019 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.run.common;

import consulo.execution.test.TestStateStorage;
import consulo.execution.lineMarker.ExecutorAction;
import consulo.execution.lineMarker.RunLineMarkerContributor;
import consulo.execution.test.TestIconMapper;
import consulo.execution.test.TestStateInfo;
import consulo.execution.icon.ExecutionIconGroup;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.document.Document;
import consulo.project.Project;
import consulo.util.io.FileUtil;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiInvalidElementAccessException;
import consulo.language.impl.ast.LeafElement;
import java.util.function.Function;
import com.jetbrains.lang.dart.DartLanguage;
import com.jetbrains.lang.dart.psi.DartCallExpression;
import consulo.language.Language;
import com.jetbrains.lang.dart.psi.DartId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import consulo.ui.image.Image;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Utility for creating {@link RunLineMarkerContributor}s for tests.
 */
public abstract class TestLineMarkerContributor extends RunLineMarkerContributor {
  private static final int SCANNED_TEST_RESULT_LIMIT = 1024;

  @NotNull
  private final CommonTestConfigUtils testConfigUtils;

  protected TestLineMarkerContributor(@NotNull CommonTestConfigUtils testConfigUtils) {
    this.testConfigUtils = testConfigUtils;
  }

  // Consulo: the language is part of the extension (upstream registers it with language="Dart")
  @NotNull
  @Override
  public Language getLanguage() {
    return DartLanguage.INSTANCE;
  }

  @Nullable
  @Override
  public Info getInfo(@NotNull PsiElement element) {
    // We look for leaf nodes of a PSI tree matching the pattern of a Dart unit test, and place
    // the line marker at that leaf node; see #4036 for some background.
    //
    // The pattern we're matching below is:
    // DartCallExpression
    //   DartReferenceExpression
    //     DartId
    //       LeafPsiElement
    if (element instanceof LeafElement && element.getParent() instanceof DartId dartId) {

      if (dartId.getParent() != null) {
        if (dartId.getParent().getParent() instanceof DartCallExpression dartCallExpression) {
          final TestType testCall = testConfigUtils.asTestCall(dartCallExpression);
          if (testCall != null) {
            final Image icon = getTestStateIcon(element, testCall.getIcon());
            final Function<PsiElement, String> tooltipProvider =
              psiElement -> testCall.getTooltip(psiElement, testConfigUtils);
            return new RunLineMarkerContributor.Info(icon, tooltipProvider, ExecutorAction.getActions());
          }
        }
      }
    }


    return null;
  }

  @NotNull
  private static Image getTestStateIcon(@NotNull PsiElement element, @NotNull Image defaultIcon) {
    // SMTTestProxy maps test run data to a URI derived from a location hint produced by `package:test`.
    // If we can find corresponding data, we can provide state-aware icons. If not, we default to
    // a standard Run state.

    PsiFile containingFile;
    try {
      containingFile = element.getContainingFile();
    }
    catch (PsiInvalidElementAccessException e) {
      containingFile = null;
    }

    final Project project = element.getProject();
    final PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(project);

    final Document document = containingFile == null ? null : psiDocumentManager.getDocument(containingFile);
    if (document != null) {
      final int textOffset = element.getTextOffset();
      final int lineNumber = document.getLineNumber(textOffset);

      // e.g., dart_location:///Users/pq/IdeaProjects/untitled1298891289891/test/unit_test.dart,3,2,["my first unit test"]
      final String path = FileUtil.toSystemIndependentName(containingFile.getVirtualFile().getPath());
      final String testLocationPrefix = "dart_location://" + path + "," + lineNumber;

      final TestStateStorage storage = TestStateStorage.getInstance(project);
      if (storage != null) {
        final Map<String, TestStateStorage.Record> tests = storage.getRecentTests(SCANNED_TEST_RESULT_LIMIT, getSinceDate());
        if (tests != null) {
          // TODO(pq): investigate performance implications.
          for (Map.Entry<String, TestStateStorage.Record> entry : tests.entrySet()) {
            if (entry.getKey().startsWith(testLocationPrefix)) {
              final TestStateStorage.Record state = entry.getValue();
              final TestStateInfo.Magnitude magnitude = TestIconMapper.getMagnitude(state.magnitude);
              if (magnitude != null) {
                switch (magnitude) {
                  case IGNORED_INDEX:
                    return PlatformIconGroup.runconfigurationsTestignored(); // Consulo: no TestState.Yellow2 gutter icon
                  case ERROR_INDEX:
                  case FAILED_INDEX:
                    return ExecutionIconGroup.gutterRunerror();
                  case PASSED_INDEX:
                  case COMPLETE_INDEX:
                    return ExecutionIconGroup.gutterRunsuccess();
                  default:
                }
              }
            }
          }
        }
      }
    }

    return defaultIcon;
  }

  private static Date getSinceDate() {
    return new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1));
  }
}
