/*
 * Copyright 2019 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.run.common;

import consulo.execution.icon.ExecutionIconGroup;
import consulo.language.psi.PsiElement;
import consulo.util.lang.StringUtil;
import org.jetbrains.annotations.NotNull;

import consulo.ui.image.Image;

/**
 * Different scopes of test that can be run on a Flutter app.
 *
 * <p>
 * Each scope corresponds to a 'test', 'group', or 'main' method declaration in a Dart test file.
 */
public enum TestType {
  // Note that mapping elements to their most specific enclosing function call depends on the ordering from most to least specific.
  SINGLE(ExecutionIconGroup.gutterRun()),
  GROUP(ExecutionIconGroup.gutterRerun()),
  /**
   * This {@link TestType} doesn't know how to detect main methods.
   * The logic to detect main methods is in {@link CommonTestConfigUtils}.
   */
  MAIN(ExecutionIconGroup.gutterRerun()) {
    @NotNull
    public String getTooltip(@NotNull PsiElement element) {
      return "Run Tests";
    }
  };

  @NotNull
  private final Image myIcon;

  TestType(@NotNull Image icon) {
    myIcon = icon;
  }

  @NotNull
  public Image getIcon() {
    return myIcon;
  }

  /**
   * Describes the tooltip to show on a particular {@param element}.
   */
  @NotNull
  public String getTooltip(@NotNull PsiElement element, @NotNull CommonTestConfigUtils testConfigUtils) {
    final String testName = testConfigUtils.findTestName(element);
    if (StringUtil.isNotEmpty(testName)) {
      return "Run '" + testName + "'";
    }

    return "Run Test";
  }
}
