/*
 * Copyright 2017 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.run.test;

import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.ConfigurationTypeBase;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.ConfigurationTypeUtil;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import io.flutter.icons.FlutterIcons;
import io.flutter.run.FlutterRunConfigurationType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * The type of configs that run tests using "flutter test".
 */
public class FlutterTestConfigType extends ConfigurationTypeBase {
  protected FlutterTestConfigType() {
    super("FlutterTestConfigType", LocalizeValue.localizeTODO("Flutter Test"), LocalizeValue.localizeTODO("description"), FlutterIcons.Flutter_test);

    addFactory(new Factory(this));
  }

  public static FlutterTestConfigType getInstance() {
    return ConfigurationTypeUtil.findConfigurationType(FlutterTestConfigType.class);
  }

  private static class Factory extends ConfigurationFactory {
    public Factory(FlutterTestConfigType type) {
      super(type);
    }

    @NotNull
    @Override
    @NonNls
    public String getId() {
      return "Flutter Test";
    }

    @NotNull
    @Override
    public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
      return new TestConfig(project, this, "Flutter Test");
    }

    @Override
    public boolean isApplicable(@NotNull Project project) {
      return FlutterRunConfigurationType.doShowFlutterRunConfigurationForProject(project);
    }
  }
}
