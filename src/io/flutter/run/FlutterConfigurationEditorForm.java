/*
 * Copyright 2016 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.run;

import com.jetbrains.lang.dart.DartFileType;
import consulo.disposer.Disposable;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.execution.configuration.fragment.RunConfigurationFragmentEditor;
import consulo.execution.configuration.fragment.RunConfigurationFragmentLayout;
import consulo.execution.ui.awt.EnvironmentVariablesTextFieldWithBrowseButton;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserTextBoxBuilder;
import consulo.localize.LocalizeValue;
import consulo.module.content.ProjectRootManager;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.process.cmd.ParametersListUtil;
import consulo.project.Project;
import consulo.ui.TextBox;
import consulo.ui.TextBoxWithExpandAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Consulo: run configurations are edited by fragments, so the upstream form ({@code FlutterConfigurationEditorForm.form_})
 * is a {@link RunConfigurationFragment} built from Consulo UI components. Hints of the form are placeholders.
 */
public class FlutterConfigurationEditorForm implements RunConfigurationFragment<SdkRunConfig> {
  @Override
  public String getId() {
    return "flutterRunConfiguration";
  }

  @Override
  public LocalizeValue getName() {
    return LocalizeValue.localizeTODO("Flutter");
  }

  @Override
  public boolean isRemovable() {
    return false;
  }

  @RequiredUIAccess
  @Override
  public RunConfigurationFragmentEditor<SdkRunConfig> createEditor(@NotNull Project project, @NotNull Disposable parent) {
    final FileChooserTextBoxBuilder.Controller myFileField = createDartFileField(project, parent);
    myFileField.getComponent().withPlaceholder(LocalizeValue.localizeTODO("The entry-point for the application (e.g. lib/main.dart)."));
    final TextBox myBuildFlavorField = TextBox.create()
      .withPlaceholder(LocalizeValue.localizeTODO("An optional build flavor; either a Gradle product flavor or an Xcode custom scheme."));
    final TextBoxWithExpandAction myAdditionalArgumentsField = createArgsField("Additional run args");
    myAdditionalArgumentsField.withPlaceholder(LocalizeValue.localizeTODO("Additional arguments to 'flutter run'."));
    final TextBoxWithExpandAction myAttachArgsField = createArgsField("Additional attach args");
    myAttachArgsField.withPlaceholder(LocalizeValue.localizeTODO("Additional arguments to 'flutter attach'."));
    final EnvironmentVariablesTextFieldWithBrowseButton myEnvironmentVariables = new EnvironmentVariablesTextFieldWithBrowseButton();

    return new RunConfigurationFragmentEditor<>() {
      @RequiredUIAccess
      @Override
      public void addRows(RunConfigurationFragmentLayout layout) {
        layout.addLabeled(LocalizeValue.localizeTODO("Dart entrypoint:"), myFileField.getComponent());
        layout.addLabeled(LocalizeValue.localizeTODO("Additional run args:"), myAdditionalArgumentsField);
        layout.addLabeled(LocalizeValue.localizeTODO("Additional attach args:"), myAttachArgsField);
        layout.addLabeled(LocalizeValue.localizeTODO("Environment variables:"), myEnvironmentVariables.getComponent());
        layout.addLabeled(LocalizeValue.localizeTODO("Build flavor:"), myBuildFlavorField);
      }

      @RequiredUIAccess
      @Override
      public void reset(@NotNull final SdkRunConfig config) {
        final SdkFields fields = config.getFields();
        myFileField.setValue(FileUtil.toSystemDependentName(StringUtil.notNullize(fields.getFilePath())), false);
        myBuildFlavorField.setValue(StringUtil.notNullize(fields.getBuildFlavor()));
        myAdditionalArgumentsField.setValue(StringUtil.notNullize(fields.getAdditionalArgs()));
        myAttachArgsField.setValue(StringUtil.notNullize(fields.getAttachArgs()));
        myEnvironmentVariables.setEnvs(fields.getEnvs());
      }

      @RequiredUIAccess
      @Override
      public void apply(@NotNull final SdkRunConfig config) {
        final SdkFields fields = new SdkFields();
        fields.setFilePath(StringUtil.nullize(FileUtil.toSystemIndependentName(myFileField.getValue().trim()), true));
        fields.setBuildFlavor(StringUtil.nullize(StringUtil.notNullize(myBuildFlavorField.getValue()).trim()));
        fields.setAdditionalArgs(StringUtil.nullize(StringUtil.notNullize(myAdditionalArgumentsField.getValue()).trim()));
        fields.setAttachArgs(StringUtil.nullize(StringUtil.notNullize(myAttachArgsField.getValue()).trim()));
        fields.setEnvs(myEnvironmentVariables.getEnvs());
        config.setFields(fields);
      }
    };
  }

  @RequiredUIAccess
  private static TextBoxWithExpandAction createArgsField(@NotNull String dialogTitle) {
    return TextBoxWithExpandAction.create(PlatformIconGroup.actionsShow(), dialogTitle, ParametersListUtil.DEFAULT_LINE_PARSER,
                                          ParametersListUtil.DEFAULT_LINE_JOINER);
  }

  /**
   * Consulo: replaces upstream {@code DartCommandLineConfigurationEditorForm.initDartFileTextWithBrowse}, which the Dart plugin
   * does not have (its run configurations are edited by fragments too).
   */
  @RequiredUIAccess
  public static FileChooserTextBoxBuilder.Controller createDartFileField(@NotNull Project project, @NotNull Disposable parent) {
    return FileChooserTextBoxBuilder.create(project)
      .dialogTitle(LocalizeValue.localizeTODO("Choose Dart File"))
      .fileChooserDescriptor(new FileChooserDescriptor(true, false, false, false, false, false)
                               .withFileFilter(file -> file.getFileType() == DartFileType.INSTANCE &&
                                                       !ProjectRootManager.getInstance(project).getFileIndex()
                                                         .isInLibraryClasses(file)))
      .uiDisposable(parent)
      .build();
  }
}
