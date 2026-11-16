/*
 * Copyright 2017 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.sdk;

import consulo.content.library.LibraryProperties;
import consulo.util.xml.serializer.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FlutterPluginLibraryProperties extends LibraryProperties<FlutterPluginLibraryProperties> {
  public FlutterPluginLibraryProperties() {
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof FlutterPluginLibraryProperties;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  @Nullable
  @Override
  public FlutterPluginLibraryProperties getState() {
    return this;
  }

  @Override
  public void loadState(@NotNull FlutterPluginLibraryProperties properties) {
    XmlSerializerUtil.copyBean(properties, this);
  }
}
