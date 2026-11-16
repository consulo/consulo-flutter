/*
 * Copyright 2016 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter;

import consulo.flutter.localize.FlutterLocalize;
import consulo.localize.LocalizeKey;
import consulo.localize.LocalizeValue;
import org.jetbrains.annotations.NotNull;

/**
 * Consulo: messages come from LOCALIZE-LIB ({@code FlutterLocalize}, generated from {@code FlutterBundle.properties}
 * by tool/consulo/sync_upstream_resources.py), so upstream keys stay valid.
 */
public class FlutterBundle {
  public static String message(@NotNull String key, @NotNull Object... params) {
    return messageValue(key, params).get();
  }

  public static @NotNull LocalizeValue messageValue(@NotNull String key, @NotNull Object... params) {
    LocalizeKey localizeKey = LocalizeKey.of(FlutterLocalize.ID, key, params.length);
    return params.length == 0 ? localizeKey.getValue() : localizeKey.getValue(params);
  }

  private FlutterBundle() {
  }
}
