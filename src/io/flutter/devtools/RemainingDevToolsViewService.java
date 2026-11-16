/*
 * Copyright 2024 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.devtools;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import consulo.project.Project;
import org.jetbrains.annotations.NotNull;

@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
public final class RemainingDevToolsViewService extends DevToolsViewService {
  @Inject
  public RemainingDevToolsViewService(@NotNull Project project) {
    super(project);
  }
}
