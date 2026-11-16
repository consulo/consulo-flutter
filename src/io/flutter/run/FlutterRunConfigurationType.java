/*
 * Copyright 2016 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.run;

import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.ConfigurationTypeBase;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.ConfigurationTypeUtil;
import consulo.project.Project;
import consulo.language.psi.search.FileTypeIndex;
import consulo.language.psi.scope.GlobalSearchScope;
import com.jetbrains.lang.dart.DartFileType;
import io.flutter.icons.FlutterIcons;
import io.flutter.FlutterBundle;
import io.flutter.utils.FlutterModuleUtils;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class FlutterRunConfigurationType extends ConfigurationTypeBase {
  private final Factory factory;

  public FlutterRunConfigurationType() {
    super("FlutterRunConfigurationType", FlutterBundle.messageValue("runner.flutter.configuration.name"),
          FlutterBundle.messageValue("runner.flutter.configuration.description"), FlutterIcons.Flutter);

    factory = new Factory(this);
    addFactory(factory);
  }

  /**
   * Defined here for all Flutter run configurations.
   */
  public static boolean doShowFlutterRunConfigurationForProject(@NotNull Project project) {
    return FileTypeIndex.containsFileOfType(DartFileType.INSTANCE, GlobalSearchScope.projectScope(project)) &&
           FlutterModuleUtils.hasFlutterModule(project);
  }

  public Factory getFactory() {
    return factory;
  }

  public static FlutterRunConfigurationType getInstance() {
    return ConfigurationTypeUtil.findConfigurationType(FlutterRunConfigurationType.class);
  }

  public static class Factory extends ConfigurationFactory {
    public Factory(FlutterRunConfigurationType type) {
      super(type);
    }

    @NotNull
    @Override
    @NonNls
    public String getId() {
      return "Flutter";
    }

    @Override
    @NotNull
    public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
      return new SdkRunConfig(project, this, "Flutter");
    }

    @Override
    @NotNull
    public RunConfiguration createConfiguration(String name, @NotNull RunConfiguration template) {
      // Override the default name which is always "Unnamed".
      return super.createConfiguration(template.getProject().getName(), template);
    }

    @Override
    public boolean isApplicable(@NotNull Project project) {
      return FlutterRunConfigurationType.doShowFlutterRunConfigurationForProject(project);
    }

    // Consulo: no RunConfigurationSingletonPolicy; MULTIPLE_INSTANCE_ONLY is "not singleton and cannot be made one"
    @Override
    public boolean isConfigurationSingletonByDefault() {
      return false;
    }

    @Override
    public boolean canConfigurationBeSingleton() {
      return false;
    }
  }
}
