/*
 * Copyright 2017 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.run.test;

import consulo.disposer.Disposable;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.execution.configuration.fragment.RunConfigurationFragmentEditor;
import consulo.execution.configuration.fragment.RunConfigurationFragmentLayout;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.fileChooser.FileChooserTextBoxBuilder;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.process.cmd.ParametersListUtil;
import consulo.project.Project;
import consulo.ui.ComboBox;
import consulo.ui.Label;
import consulo.ui.StaticPosition;
import consulo.ui.TextBox;
import consulo.ui.TextBoxWithExpandAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.TableLayout;
import consulo.util.lang.StringUtil;
import io.flutter.run.FlutterConfigurationEditorForm;
import io.flutter.run.test.TestFields.Scope;
import org.jetbrains.annotations.NotNull;

import java.io.File;

import static io.flutter.run.test.TestFields.Scope.*;

/**
 * Settings editor for running Flutter tests.
 * <p>
 * Consulo: run configurations are edited by fragments, so the upstream form ({@code TestForm.form_}) is a
 * {@link RunConfigurationFragment} built from Consulo UI components.
 */
public class TestForm implements RunConfigurationFragment<TestConfig> {
  TestForm() {
  }

  @Override
  public String getId() {
    return "flutterTest";
  }

  @Override
  public LocalizeValue getName() {
    return LocalizeValue.localizeTODO("Flutter Test");
  }

  @Override
  public boolean isRemovable() {
    return false;
  }

  @RequiredUIAccess
  @Override
  public RunConfigurationFragmentEditor<TestConfig> createEditor(@NotNull Project project, @NotNull Disposable parent) {
    return new Editor(project, parent);
  }

  private static class Editor implements RunConfigurationFragmentEditor<TestConfig> {
    private final TableLayout form;

    private final ComboBox<Scope> scope;

    private final Label testDirLabel;
    private final FileChooserTextBoxBuilder.Controller testDir;
    private final Label testDirHintLabel;

    private final Label testFileLabel;
    private final FileChooserTextBoxBuilder.Controller testFile;
    private final Label testFileHintLabel;

    private final Label testNameLabel;
    private final TextBox testName;
    private final Label testNameHintLabel;

    private final TextBoxWithExpandAction additionalArgs;

    private Scope displayedScope;

    @RequiredUIAccess
    Editor(@NotNull Project project, @NotNull Disposable parent) {
      scope = ComboBox.create(DIRECTORY, FILE, NAME);
      scope.addValueListener(event -> {
        final Scope next = getScope();
        updateFields(next);
        render(next);
      });
      scope.setTextRenderer(value -> value == null ? LocalizeValue.empty() : LocalizeValue.of(value.getDisplayName()));

      testFile = FlutterConfigurationEditorForm.createDartFileField(project, parent);
      testDir = FileChooserTextBoxBuilder.create(project)
        .dialogTitle(LocalizeValue.localizeTODO("Test Directory"))
        .fileChooserDescriptor(FileChooserDescriptorFactory.createSingleFolderDescriptor())
        .uiDisposable(parent)
        .build();
      testName = TextBox.create();
      additionalArgs = TextBoxWithExpandAction.create(PlatformIconGroup.actionsShow(), "Additional args",
                                                      ParametersListUtil.DEFAULT_LINE_PARSER, ParametersListUtil.DEFAULT_LINE_JOINER);

      testDirLabel = Label.create(LocalizeValue.localizeTODO("Test directory:"));
      testDirHintLabel = Label.create(LocalizeValue.localizeTODO("The directory scoping the run tests (e.g. '/.../test/')."));
      testFileLabel = Label.create(LocalizeValue.localizeTODO("Test file:"));
      testFileHintLabel = Label.create(LocalizeValue.localizeTODO("The entry-point for the test (e.g. 'test/simple_test.dart')."));
      testNameLabel = Label.create(LocalizeValue.localizeTODO("Test name:"));
      testNameHintLabel = Label.create(LocalizeValue.localizeTODO("Some pattern to match test names by."));

      // Consulo: the rows of the upstream form; labels and hints are hidden with their fields, so this is one table.
      form = TableLayout.create(StaticPosition.TOP);
      int row = 0;
      form.add(Label.create(LocalizeValue.localizeTODO("Test scope:")), TableLayout.cell(row, 0));
      form.add(scope, TableLayout.cell(row++, 1).fill());
      form.add(Label.create(LocalizeValue.localizeTODO(
        "Scope of the test: either file, directory, or tests in a file and filtered by name.")), TableLayout.cell(row++, 1));
      form.add(testDirLabel, TableLayout.cell(row, 0));
      form.add(testDir.getComponent(), TableLayout.cell(row++, 1).fill());
      form.add(testDirHintLabel, TableLayout.cell(row++, 1));
      form.add(testFileLabel, TableLayout.cell(row, 0));
      form.add(testFile.getComponent(), TableLayout.cell(row++, 1).fill());
      form.add(testFileHintLabel, TableLayout.cell(row++, 1));
      form.add(testNameLabel, TableLayout.cell(row, 0));
      form.add(testName, TableLayout.cell(row++, 1).fill());
      form.add(testNameHintLabel, TableLayout.cell(row++, 1));
      form.add(Label.create(LocalizeValue.localizeTODO("Additional args:")), TableLayout.cell(row, 0));
      form.add(additionalArgs, TableLayout.cell(row++, 1).fill());
      form.add(Label.create(LocalizeValue.localizeTODO("Additional arguments to pass to the test runner.")), TableLayout.cell(row, 1));
    }

    @RequiredUIAccess
    @Override
    public void addRows(RunConfigurationFragmentLayout layout) {
      layout.addUnlabeled(form);
    }

    @RequiredUIAccess
    @Override
    public void reset(@NotNull TestConfig config) {
      final TestFields fields = config.getFields();
      final Scope next = fields.getScope();
      scope.setValue(next);
      switch (next) {
        case NAME:
          testName.setValue(StringUtil.notNullize(fields.getTestName()));
          // fallthrough
        case FILE:
          testFile.setValue(StringUtil.notNullize(fields.getTestFile()), false);
          break;
        case DIRECTORY:
          testDir.setValue(StringUtil.notNullize(fields.getTestDir()), false);
          break;
      }
      additionalArgs.setValue(StringUtil.notNullize(fields.getAdditionalArgs()));
      render(next);
    }

    @RequiredUIAccess
    @Override
    public void apply(@NotNull TestConfig config) {
      final TestFields fields = switch (getScope()) {
        case NAME -> TestFields.forTestName(testName.getValue(), testFile.getValue());
        case FILE -> TestFields.forFile(testFile.getValue());
        case DIRECTORY -> TestFields.forDir(testDir.getValue());
      };
      fields.setAdditionalArgs(StringUtil.notNullize(additionalArgs.getValue()).trim());
      config.setFields(fields);
    }

    @NotNull
    private Scope getScope() {
      final Scope item = scope.getValue();
      // Set in resetEditorForm.
      assert (item != null);
      return item;
    }

    /**
     * When switching between file and directory scope, update the next field to
     * a suitable default.
     */
    @RequiredUIAccess
    private void updateFields(Scope next) {
      if (next == Scope.DIRECTORY && displayedScope != Scope.DIRECTORY) {
        final String sep = String.valueOf(File.separatorChar);

        final String path = testFile.getValue();
        if (path.contains(sep) && path.endsWith(".dart")) {
          // Remove the last part of the path to get a directory.
          testDir.setValue(path.substring(0, path.lastIndexOf(sep) + 1), false);
        }
        else if (testDir.getValue().isEmpty()) {
          // Keep the same path; better than starting blank.
          testDir.setValue(path, false);
        }
      }
      else if (next != Scope.DIRECTORY && displayedScope == Scope.DIRECTORY) {
        if (testFile.getValue().isEmpty()) {
          testFile.setValue(testDir.getValue(), false);
        }
      }
    }

    /**
     * Show and hide fields as appropriate for the next scope.
     */
    @RequiredUIAccess
    private void render(Scope next) {

      testDirLabel.setVisible(next == Scope.DIRECTORY);
      testDirHintLabel.setVisible(next == Scope.DIRECTORY);
      testDir.getComponent().setVisible(next == Scope.DIRECTORY);

      testFileLabel.setVisible(next != Scope.DIRECTORY);
      testFileHintLabel.setVisible(next != Scope.DIRECTORY);
      testFile.getComponent().setVisible(next != Scope.DIRECTORY);

      testNameLabel.setVisible(next == Scope.NAME);
      testNameHintLabel.setVisible(next == Scope.NAME);
      testName.setVisible(next == Scope.NAME);

      displayedScope = next;
    }
  }
}
