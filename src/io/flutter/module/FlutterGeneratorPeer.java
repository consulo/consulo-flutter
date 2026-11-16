/*
 * Copyright 2016 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.module;

import consulo.application.AllIcons;
import consulo.application.ApplicationManager;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.ui.ex.awt.ComboBox;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.TextComponentAccessor;
import consulo.ui.ex.awt.ValidationInfo;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.ui.ex.awt.ComboboxWithBrowseButton;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.update.UiNotifyConnector;
import consulo.util.lang.xml.XmlStringUtil;
import io.flutter.FlutterBundle;
import io.flutter.module.settings.SettingsHelpForm;
import io.flutter.sdk.FlutterSdk;
import io.flutter.sdk.FlutterSdkUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.ComboBoxEditor;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.event.DocumentEvent;
import javax.swing.text.JTextComponent;

public class FlutterGeneratorPeer {
  private JPanel myMainPanel;
  private ComboboxWithBrowseButton mySdkPathComboWithBrowse;
  private JBLabel myVersionContent;
  private JLabel errorIcon;
  private JTextPane errorText;
  private JScrollPane errorPane;
  private SettingsHelpForm myHelpForm;

  // Consulo: there is no IntelliJ WizardContext; upstream only stored it
  public FlutterGeneratorPeer() {

    errorIcon.setText("");
    errorIcon.setIcon(TargetAWT.to(AllIcons.Actions.Lightning));
    Messages.installHyperlinkSupport(errorText);

    // Hide pending real content.
    myVersionContent.setVisible(false);

    // TODO(messick) Remove this field.
    myHelpForm.getComponent().setVisible(false);

    init();
  }

  private void init() {
    final JComboBox<String> sdkCombo = mySdkPathComboWithBrowse.getComboBox();
    sdkCombo.setEditable(true);

    // The combo may not be showing yet (e.g. switching project type mid-wizard); wait until
    // it is so FlutterSdkUtil can resolve the dialog's real modality state.
    UiNotifyConnector.doWhenFirstShown(sdkCombo, () -> populateSdkCombo(sdkCombo));

    mySdkPathComboWithBrowse.addBrowseFolderListener(FlutterBundle.messageValue("flutter.sdk.browse.path.label"), LocalizeValue.empty(), null,
                                                     FileChooserDescriptorFactory.createSingleFolderDescriptor(),
                                                     TextComponentAccessor.STRING_COMBOBOX_WHOLE_TEXT);
    mySdkPathComboWithBrowse.getComboBox().addActionListener(e -> fillSdkCache());
    fillSdkCache();

    final JTextComponent editorComponent = (JTextComponent)getSdkEditor().getEditorComponent();
    editorComponent.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent e) {
        validate();
      }
    });

    errorIcon.setVisible(false);
    errorPane.setVisible(false);
  }

  private void populateSdkCombo(@NotNull JComboBox<String> combo) {
    FlutterSdkUtil.addKnownSDKPathsToCombo(combo);
    if (combo.getModel().getSize() == 0) {
      // If no SDKs are found, try to use the one from the FLUTTER_SDK environment variable.
      // This ensures the SDK path is pre-filled when the combo box is empty, not requiring
      // a running Application which is the case for users and bots on the initial startup
      // experience.
      final String flutterSDKPath = System.getenv("FLUTTER_SDK");
      if (StringUtil.isNotEmpty(flutterSDKPath)) {
        combo.setSelectedItem(flutterSDKPath);
      }
    }
  }

  private void fillSdkCache() {
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      String path = (String)mySdkPathComboWithBrowse.getComboBox().getSelectedItem();
      if (path != null) {
        FlutterSdk sdk = FlutterSdk.forPath(path);
        if (sdk != null) {
          sdk.queryConfiguredPlatforms(false);
        }
      }
    });
  }

  @SuppressWarnings("EmptyMethod")
  void apply() {
  }

  @NotNull
  public JComponent getComponent() {
    return myMainPanel;
  }

  private void createUIComponents() {
    mySdkPathComboWithBrowse = new ComboboxWithBrowseButton(new ComboBox<>());
  }

  // TODO Link this to actual validation.
  public boolean validate() {
    final ValidationInfo info = validateSdk();
    if (info != null) {
      errorText.setText(XmlStringUtil.wrapInHtml(info.message.get()));
    }
    errorIcon.setVisible(info != null);
    errorPane.setVisible(info != null);

    return info == null;
  }

  @Nullable
  private ValidationInfo validateSdk() {
    final String sdkPath = getSdkComboPath();
    if (StringUtil.isEmpty(sdkPath)) {
      return new ValidationInfo("A Flutter SDK must be specified for project creation.", mySdkPathComboWithBrowse);
    }
    final String message = FlutterSdkUtil.getErrorMessageIfWrongSdkRootPath(sdkPath);
    if (message != null) {
      return new ValidationInfo(message, mySdkPathComboWithBrowse);
    }

    return null;
  }

  @NotNull
  public String getSdkComboPath() {
    return FileUtil.toSystemIndependentName(getSdkEditor().getItem().toString().trim());
  }

  @NotNull
  public ComboBoxEditor getSdkEditor() {
    return mySdkPathComboWithBrowse.getComboBox().getEditor();
  }

  public SettingsHelpForm getHelpForm() {
    return myHelpForm;
  }
}
