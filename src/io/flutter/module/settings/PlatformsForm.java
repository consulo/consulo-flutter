/*
 * Copyright 2020 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.module.settings;

import consulo.ui.ex.awt.JBCheckBox;
import consulo.ui.ex.awt.VerticalFlowLayout;
import io.flutter.FlutterBundle;
import io.flutter.sdk.FlutterCreateAdditionalSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.FlowLayout;
import java.util.List;

// Consulo: upstream builds the panel with the Kotlin UI DSL; here it is a plain Swing panel with the same rows
public class PlatformsForm {
  private boolean configAndroid = true;
  private boolean configIos = true;
  private boolean configLinux = true;
  private boolean configMacos = true;
  private boolean configWeb = true;
  private boolean configWindows = true;
  @Nullable
  private JPanel component;

  @Nullable
  public JPanel getComponent() {
    return component;
  }

  public void setComponent(@Nullable JPanel component) {
    this.component = component;
  }

  @NotNull
  public JPanel panel(@NotNull FlutterCreateAdditionalSettings settings) {
    final JPanel checkBoxRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    makeCheckBox(checkBoxRow, FlutterBundle.message("npw_platform_android"), settings.getPlatformAndroidProperty(), configAndroid);
    makeCheckBox(checkBoxRow, FlutterBundle.message("npw_platform_ios"), settings.getPlatformIosProperty(), configIos);
    makeCheckBox(checkBoxRow, FlutterBundle.message("npw_platform_linux"), settings.getPlatformLinuxProperty(), configLinux);
    makeCheckBox(checkBoxRow, FlutterBundle.message("npw_platform_macos"), settings.getPlatformMacosProperty(), configMacos);
    makeCheckBox(checkBoxRow, FlutterBundle.message("npw_platform_web"), settings.getPlatformWebProperty(), configWeb);
    makeCheckBox(checkBoxRow, FlutterBundle.message("npw_platform_windows"), settings.getPlatformWindowsProperty(), configWindows);

    final JPanel panel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 0, true, false));
    panel.add(checkBoxRow);
    panel.add(new JLabel(FlutterBundle.message("npw_platform_selection_help")));
    component = panel;
    return panel;
  }

  private static void makeCheckBox(@NotNull JPanel row,
                                   @NotNull String name,
                                   @NotNull InitializeOnceBoolValueProperty property,
                                   @Nullable Boolean config) {
    final boolean wasSelected = config == Boolean.TRUE;
    property.initialize(wasSelected);

    final List<String> names = List.of(
      FlutterBundle.message("npw_platform_android"),
      FlutterBundle.message("npw_platform_ios"),
      FlutterBundle.message("npw_platform_web"),
      FlutterBundle.message("npw_platform_windows"),
      FlutterBundle.message("npw_platform_linux"),
      FlutterBundle.message("npw_platform_macos")
    );

    final JBCheckBox checkBox = new JBCheckBox(name);
    checkBox.addActionListener(e -> property.set(checkBox.isSelected()));
    checkBox.setSelected(property.get());
    checkBox.setEnabled(names.contains(name) || wasSelected);
    row.add(checkBox);
  }
}
