/*
 * Copyright 2024 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.deeplinks;

import consulo.annotation.component.ExtensionImpl;

import consulo.project.Project;
import consulo.ui.image.Image;
import io.flutter.icons.FlutterIcons;
import io.flutter.devtools.AbstractDevToolsViewFactory;
import io.flutter.devtools.DevToolsIdeFeature;
import io.flutter.devtools.DevToolsUrl;
import io.flutter.run.daemon.DevToolsInstance;
import io.flutter.sdk.FlutterSdkVersion;
import org.jetbrains.annotations.NotNull;

@ExtensionImpl
public class DeepLinksViewFactory extends AbstractDevToolsViewFactory {
  @NotNull public static String TOOL_WINDOW_ID = "Flutter Deep Links";

  @NotNull public static String DEVTOOLS_PAGE_ID = "deep-links";

  @Override
  public boolean versionSupportsThisTool(@NotNull final FlutterSdkVersion flutterSdkVersion) {
    return true;
  }

  @Override
  @NotNull
  public String getToolWindowId() {
    return TOOL_WINDOW_ID;
  }

  // Consulo: the tool window icon of upstream plugin.xml
  @Override
  @NotNull
  public Image getIcon() {
    return FlutterIcons.DevToolsDeepLinks;
  }

  @Override
  @NotNull
  public String getToolWindowTitle() {
    return "Deep Links";
  }

  @Override
  @NotNull
  public DevToolsUrl getDevToolsUrl(@NotNull final Project project,
                                    @NotNull final FlutterSdkVersion flutterSdkVersion,
                                    @NotNull final DevToolsInstance instance) {
    return new DevToolsUrl.Builder()
      .setDevToolsHost(instance.host())
      .setDevToolsPort(instance.port())
      .setPage(DEVTOOLS_PAGE_ID)
      .setEmbed(true)
      .setFlutterSdkVersion(flutterSdkVersion)
      .setIdeFeature(DevToolsIdeFeature.TOOL_WINDOW)
      .build();
  }
}
