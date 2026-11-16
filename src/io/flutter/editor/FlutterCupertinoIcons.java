/*
 * Copyright 2018 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.editor;

import consulo.logging.Logger;
import consulo.ui.image.Image;
import io.flutter.FlutterUtils;
import io.flutter.logging.PluginLogger;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.io.IOException;
import java.util.Properties;

public class FlutterCupertinoIcons {
  private static final @NotNull Logger LOG = PluginLogger.createLogger(FlutterCupertinoIcons.class);

  private static final Properties icons;

  static {
    icons = new Properties();

    try {
      icons.load(FlutterCupertinoIcons.class.getResourceAsStream("/flutter/icons/cupertino.properties"));
    }
    catch (IOException e) {
      FlutterUtils.warn(LOG, "Failed to load cupertino icons", e, true);
    }
  }

  public static Image getIconForHex(String hexValue) {
    final String iconName = icons.getProperty(hexValue + ".codepoint");
    return getIcon(iconName);
  }

  public static Image getIconForName(String name) {
    return getIcon(name);
  }

  private static Image getIcon(String name) {
    if (name == null) {
      return null;
    }
    final String path = icons.getProperty(name);
    if (path == null) {
      return null;
    }
    // Consulo: Image.fromUrl instead of IconLoader.findIcon
    final URL url = FlutterCupertinoIcons.class.getResource("/flutter/icons/" + path);
    if (url == null) {
      return null;
    }
    try {
      return Image.fromUrl(url);
    }
    catch (IOException e) {
      return null;
    }
  }
}
