/*
 * Copyright 2026 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter;

import consulo.disposer.Disposable;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import jakarta.inject.Singleton;
import consulo.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * Project-level {@link Disposable} parent for Flutter plugin resources.
 */
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
public final class FlutterProjectDisposable implements Disposable {
  @NotNull
  public static Disposable getInstance(@NotNull Project project) {
    return project.getInstance(FlutterProjectDisposable.class);
  }

  @Override
  public void dispose() {
  }
}
