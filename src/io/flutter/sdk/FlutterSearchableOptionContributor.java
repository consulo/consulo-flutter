/*
 * Copyright 2018 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.sdk;

import consulo.configurable.search.SearchableOptionContributor;
import consulo.configurable.search.SearchableOptionProcessor;
import consulo.localize.LocalizeValue;
import io.flutter.FlutterBundle;
import io.flutter.FlutterConstants;
import org.jetbrains.annotations.NotNull;

// Note: update this file when updating FlutterSettingsConfigurable.

public class FlutterSearchableOptionContributor extends SearchableOptionContributor {
  @Override
  public void processOptions(@NotNull SearchableOptionProcessor processor) {
    // Keep these in the same order as FlutterBundle.properties
    add(processor, FlutterBundle.messageValue("settings.try.out.features.still.under.development"));
    add(processor, FlutterBundle.messageValue("settings.enable.verbose.logging"));
    add(processor, FlutterBundle.messageValue("settings.format.code.on.save"));
    add(processor, FlutterBundle.messageValue("settings.organize.imports.on.save"));
    add(processor, FlutterBundle.messageValue("settings.flutter.version"));
    add(processor, FlutterBundle.messageValue("settings.open.inspector.on.launch"));
    add(processor, FlutterBundle.messageValue("settings.hot.reload.on.save"));
    add(processor, FlutterBundle.messageValue("settings.allow.tests.in.sources"));
    add(processor, FlutterBundle.messageValue("settings.allow.tests.tooltip"));
    add(processor, FlutterBundle.messageValue("settings.font.packages"));
    add(processor, FlutterBundle.messageValue("settings.experiments"));
    add(processor, FlutterBundle.messageValue("settings.editor"));
    add(processor, FlutterBundle.messageValue("settings.show.build.guides"));
    add(processor, FlutterBundle.messageValue("settings.show.build.guides.tooltip"));
    add(processor, FlutterBundle.messageValue("settings.format.code.tooltip"));
    add(processor, FlutterBundle.messageValue("settings.organize.imports.tooltip"));
    add(processor, FlutterBundle.messageValue("settings.show.closing.labels"));
    add(processor, FlutterBundle.messageValue("settings.sdk.copy.content"));
    add(processor, FlutterBundle.messageValue("settings.enable.verbose.logging.tooltip"));
  }

  private static void add(@NotNull SearchableOptionProcessor processor, @NotNull LocalizeValue key) {
    processor.addOption(key, key, null, FlutterConstants.FLUTTER_SETTINGS_PAGE_ID,
                        LocalizeValue.of(FlutterSettingsConfigurable.FLUTTER_SETTINGS_PAGE_NAME));
  }
}
