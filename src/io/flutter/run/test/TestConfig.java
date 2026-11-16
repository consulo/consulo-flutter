/*
 * Copyright 2017 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.run.test;

import consulo.process.ExecutionException;
import consulo.execution.executor.Executor;
import consulo.execution.configuration.CommandLineState;
import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.LocatableConfigurationBase;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.RuntimeConfigurationException;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.test.TestConsoleProperties;
import com.intellij.execution.testframework.actions.ConsolePropertiesProvider;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.project.Project;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import io.flutter.analytics.Analytics;
import io.flutter.analytics.AnalyticsConstants;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A configuration for running Flutter tests.
 * <p>
 * Inheriting from ConsolePropertiesProvider enables the auto-test-before-commit feature in the VCS tool window.
 * Note that using that window causes additional analysis to occur, which creates a bunch of spurious errors.
 * IntelliJ has its own rules for Android files, and Flutter doesn't follow some of them.
 */
public class TestConfig extends LocatableConfigurationBase<CommandLineState> implements ConsolePropertiesProvider {
  @NotNull
  private TestFields fields = TestFields.forFile("");

  protected TestConfig(@NotNull Project project,
                       @NotNull ConfigurationFactory factory, String name) {
    super(project, factory, name);
  }

  @NotNull
  public TestFields getFields() {
    return fields;
  }

  public void setFields(@NotNull TestFields fields) {
    this.fields = fields;
  }

  @Override
  public void writeExternal(@NotNull Element element) throws WriteExternalException {
    super.writeExternal(element);
    fields.writeTo(element);
  }

  @Override
  public void readExternal(@NotNull Element element) throws InvalidDataException {
    super.readExternal(element);
    fields = TestFields.readFrom(element);
  }

  @Override
  public RunConfiguration clone() {
    final TestConfig result = (TestConfig)super.clone();
    result.fields = fields;
    return result;
  }

  @NotNull
  @Override
  public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    return new TestForm(getProject());
  }

  @Nullable
  @Override
  public String suggestedName() {
    return fields.getSuggestedName(getProject(), "Flutter Tests");
  }

  @Override
  public void checkConfiguration() throws RuntimeConfigurationException {
    fields.checkRunnable(getProject());
  }

  @NotNull
  @Override
  public CommandLineState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env) throws ExecutionException {
    Analytics.recordRunOrDebugSession(AnalyticsConstants.MECHANISM_FLUTTER_TESTS, executor, getProject());
    return TestLaunchState.create(env, this);
  }

  @Nullable
  @Override
  public TestConsoleProperties createTestConsoleProperties(@NotNull Executor exec) {
    return new FlutterTestConsoleProperties(this, exec);
  }
}
