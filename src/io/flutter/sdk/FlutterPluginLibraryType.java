/*
 * Copyright 2017 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.sdk;

import consulo.project.Project;
import consulo.content.library.LibraryType;
import consulo.content.library.NewLibraryConfiguration;
import consulo.content.library.PersistentLibraryKind;
import consulo.content.library.ui.LibraryEditorComponent;
import consulo.content.library.ui.LibraryPropertiesEditor;
import consulo.virtualFileSystem.VirtualFile;
import io.flutter.icons.FlutterIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class FlutterPluginLibraryType extends LibraryType<FlutterPluginLibraryProperties> {
  public static final String FLUTTER_PLUGINS_LIBRARY_NAME = "Flutter Plugins";

  public static final PersistentLibraryKind<FlutterPluginLibraryProperties> LIBRARY_KIND =
    new PersistentLibraryKind<>("FlutterPluginsLibraryType") {
      @Override
      @NotNull
      public FlutterPluginLibraryProperties createDefaultProperties() {
        return new FlutterPluginLibraryProperties();
      }
    };

  protected FlutterPluginLibraryType() {
    super(LIBRARY_KIND);
  }

  @Nullable
  @Override
  public String getCreateActionName() {
    return null;
  }

  @Nullable
  @Override
  public NewLibraryConfiguration createNewLibrary(@NotNull JComponent component, @Nullable VirtualFile file, @NotNull Project project) {
    return null;
  }

  @Nullable
  @Override
  public LibraryPropertiesEditor createPropertiesEditor(@NotNull LibraryEditorComponent<FlutterPluginLibraryProperties> component) {
    return null;
  }

  @Nullable
  public Icon getIcon(@Nullable FlutterPluginLibraryProperties properties) {
    return FlutterIcons.Flutter;
  }
}
