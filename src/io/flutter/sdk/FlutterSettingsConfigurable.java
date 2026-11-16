/*
 * Copyright 2016 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.sdk;

import consulo.process.util.CapturingProcessAdapter;
import consulo.process.ExecutionException;
import consulo.process.ProcessHandler;
import consulo.process.ProcessHandlerBuilder;
import consulo.process.util.ProcessOutput;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.flutter.notification.FlutterSdkNotificationGroups;
import consulo.project.ui.notification.NotificationService;
import consulo.ui.ex.action.ActionToolbar;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.ui.ModalityState;
import consulo.fileChooser.FileChooser;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.ui.ex.awt.CopyPasteManager;
import consulo.configurable.ConfigurationException;
import consulo.configurable.SearchableConfigurable;
import consulo.project.Project;
import consulo.ui.ex.awt.ComboBox;
import consulo.ui.ex.awt.FixedSizeButton;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.ui.ex.awt.JBLabel;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.awt.JBTextField;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import io.flutter.icons.FlutterIcons;
import io.flutter.FlutterBundle;
import io.flutter.FlutterConstants;
import io.flutter.FlutterMessages;
import io.flutter.font.FontPreviewProcessor;
import io.flutter.pub.PubRoot;
import io.flutter.pub.PubRoots;
import io.flutter.settings.FlutterSettings;
import io.flutter.utils.OpenApiUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.datatransfer.StringSelection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

// Note: when updating the settings here, update FlutterSearchableOptionContributor as well.

public class FlutterSettingsConfigurable implements SearchableConfigurable {

  public static final String FLUTTER_SETTINGS_PAGE_NAME = FlutterBundle.message("flutter.title");
  private static final String FLUTTER_SETTINGS_HELP_TOPIC = "flutter.settings.help";

  private JPanel mainPanel;
  private ComboBox<String> mySdkCombo;
  private JBLabel myVersionLabel;
  private JCheckBox myHotReloadOnSaveCheckBox;
  private JCheckBox myEnableVerboseLoggingCheckBox;
  private JCheckBox myOpenInspectorOnAppLaunchCheckBox;
  private JCheckBox myFormatCodeOnSaveCheckBox;
  private JCheckBox myOrganizeImportsOnSaveCheckBox;
  private JCheckBox myShowStructuredErrors;
  private JCheckBox myIncludeAllStackTraces;

  private JCheckBox myEnableJcefBrowserCheckBox;
  private JCheckBox myShowBuildMethodGuides;
  private JCheckBox myShowClosingLabels;
  private FixedSizeButton myCopyButton;
  private JTextArea myFontPackagesTextArea; // This should be changed to a structured list some day.
  private JCheckBox myAllowTestsInSourcesRoot;
  // Consulo: there is no "Actions on Save" settings page, so the link to it (settingsLink) is removed from the form
  private JCheckBox myEnableLogsPreserveAfterHotReloadOrRestart;
  private @NotNull JCheckBox myEnableFilePathLogging;

  private final @NotNull Project myProject;

  private boolean ignoringSdkChanges = false;
  private String fullVersionString;
  private FlutterSdkVersion previousSdkVersion;

  private static final Logger LOG = Logger.getInstance(FlutterSettingsConfigurable.class);

  // Consulo: a ProcessHandler can't wrap an already started Process, so the running `flutter --version` is tracked by its handler
  private final AtomicReference<ProcessHandler> activeVersionProcess = new AtomicReference<>();

  FlutterSettingsConfigurable(@NotNull Project project) {
    this.myProject = project;
    init();
    myVersionLabel.setText(" ");
  }

  private void init() {
    final FlutterSdk sdk = FlutterSdk.getFlutterSdk(myProject);
    if (sdk != null) {
      previousSdkVersion = sdk.getVersion();
    }
    mySdkCombo.setEditable(true);

    myCopyButton.setSize(TargetAWT.to(ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE));
    myCopyButton.setIcon(TargetAWT.to(PlatformIconGroup.actionsCopy()));
    myCopyButton.addActionListener(e -> {
      if (fullVersionString != null) {
        CopyPasteManager.getInstance().setContents(new StringSelection(fullVersionString));
      }
    });

    final JTextComponent sdkEditor = (JTextComponent)mySdkCombo.getEditor().getEditorComponent();
    sdkEditor.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull final DocumentEvent e) {
        if (!ignoringSdkChanges) {
          onVersionChanged();
        }
      }
    });
    myFormatCodeOnSaveCheckBox.addChangeListener(
      (e) -> myOrganizeImportsOnSaveCheckBox.setEnabled(myFormatCodeOnSaveCheckBox.isSelected()));
    myShowStructuredErrors.addChangeListener(
      (e) -> myIncludeAllStackTraces.setEnabled(myShowStructuredErrors.isSelected()));
  }

  private void createUIComponents() {
    // Consulo: ExtendableTextField is not an API, the browse button is placed inside the editor text field instead
    final JBTextField ecbEditor = new JBTextField();
    ecbEditor.setBorder(null);
    final FixedSizeButton browseButton = new FixedSizeButton(ecbEditor);
    browseButton.setIcon(TargetAWT.to(PlatformIconGroup.nodesFolder()));
    //noinspection DialogTitleCapitalization
    browseButton.setToolTipText("Select Flutter SDK Path");
    browseButton.addActionListener(e -> {
      FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
      FileChooser.chooseFile(descriptor, mySdkCombo, null, null).thenAccept(file -> {
        if (file != null) {
          //noinspection DataFlowIssue (sure to be set before the extension is invoked)
          mySdkCombo.getEditor().setItem(file.getPath());
        }
      });
    });
    ecbEditor.setLayout(new BorderLayout());
    ecbEditor.add(browseButton, BorderLayout.EAST);

    mySdkCombo = new ComboBox<>();
    mySdkCombo.setEditor(new BasicComboBoxEditor() {
      @Override
      protected JTextField createEditorComponent() {
        return ecbEditor;
      }
    });
  }

  @Override
  @NotNull
  public String getId() {
    return FlutterConstants.FLUTTER_SETTINGS_PAGE_ID;
  }

  // Consulo: configurables return a Consulo UI component, the Swing panel is wrapped
  @RequiredUIAccess
  @Nullable
  @Override
  public Component createUIComponent(@NotNull Disposable parentDisposable) {
    return TargetAWT.wrap(mainPanel);
  }

  @Override
  public boolean isModified() {

    final FlutterSdk sdk = FlutterSdk.getFlutterSdk(myProject);
    final FlutterSettings settings = FlutterSettings.getInstance();
    final String sdkPathInModel = sdk == null ? "" : sdk.getHomePath();
    final String sdkPathInUI = FileUtil.toSystemIndependentName(getSdkPathText());

    if (!sdkPathInModel.equals(sdkPathInUI)) {
      return true;
    }

    if (settings.isReloadOnSave() != myHotReloadOnSaveCheckBox.isSelected()) {
      return true;
    }

    if (settings.isFormatCodeOnSave() != myFormatCodeOnSaveCheckBox.isSelected()) {
      return true;
    }

    if (settings.isOrganizeImportsOnSave() != myOrganizeImportsOnSaveCheckBox.isSelected()) {
      return true;
    }

    if (settings.isShowBuildMethodGuides() != myShowBuildMethodGuides.isSelected()) {
      return true;
    }

    if (settings.isShowClosingLabels() != myShowClosingLabels.isSelected()) {
      return true;
    }

    if (settings.isShowStructuredErrors() != myShowStructuredErrors.isSelected()) {
      return true;
    }

    if (settings.isIncludeAllStackTraces() != myIncludeAllStackTraces.isSelected()) {
      return true;
    }

    if (settings.isOpenInspectorOnAppLaunch() != myOpenInspectorOnAppLaunchCheckBox.isSelected()) {
      return true;
    }

    if (settings.isPerserveLogsDuringHotReloadAndRestart() != myEnableLogsPreserveAfterHotReloadOrRestart.isSelected()) {
      return true;
    }

    if (settings.isVerboseLogging() != myEnableVerboseLoggingCheckBox.isSelected()) {
      return true;
    }

    if (settings.isAllowTestsInSourcesRoot() != myAllowTestsInSourcesRoot.isSelected()) {
      return true;
    }

    if (!Objects.equals(settings.getFontPackages(), myFontPackagesTextArea.getText())) {
      return true;
    }

    if (settings.isFilePathLoggingEnabled() != myEnableFilePathLogging.isSelected()) {
      return true;
    }

    return settings.isEnableJcefBrowser() != myEnableJcefBrowserCheckBox.isSelected();
  }

  @Override
  public void apply() throws ConfigurationException {
    final String errorMessage = FlutterSdkUtil.getErrorMessageIfWrongSdkRootPath(getSdkPathText());
    if (errorMessage != null) {
      throw new ConfigurationException(errorMessage);
    }

    final String sdkHomePath = getSdkPathText();
    if (FlutterSdkUtil.isFlutterSdkHome(sdkHomePath)) {
      OpenApiUtils.safeRunWriteAction(() -> {
        FlutterSdkUtil.setFlutterSdkPath(myProject, sdkHomePath);
        FlutterSdkUtil.enableDartSdk(myProject);
      });
    }

    final FlutterSettings settings = FlutterSettings.getInstance();
    final String oldFontPackages = settings.getFontPackages();
    settings.setReloadOnSave(myHotReloadOnSaveCheckBox.isSelected());
    settings.setFormatCodeOnSave(myFormatCodeOnSaveCheckBox.isSelected());
    settings.setOrganizeImportsOnSave(myOrganizeImportsOnSaveCheckBox.isSelected());

    settings.setShowBuildMethodGuides(myShowBuildMethodGuides.isSelected());
    settings.setShowClosingLabels(myShowClosingLabels.isSelected());
    settings.setShowStructuredErrors(myShowStructuredErrors.isSelected());
    settings.setIncludeAllStackTraces(myIncludeAllStackTraces.isSelected());
    settings.setOpenInspectorOnAppLaunch(myOpenInspectorOnAppLaunchCheckBox.isSelected());
    settings.setPerserveLogsDuringHotReloadAndRestart(myEnableLogsPreserveAfterHotReloadOrRestart.isSelected());
    settings.setVerboseLogging(myEnableVerboseLoggingCheckBox.isSelected());
    settings.setAllowTestsInSourcesRoot(myAllowTestsInSourcesRoot.isSelected());
    settings.setFontPackages(myFontPackagesTextArea.getText());
    settings.setEnableJcefBrowser(myEnableJcefBrowserCheckBox.isSelected());
    settings.setFilePathLoggingEnabled(myEnableFilePathLogging.isSelected());

    reset(); // because we rely on remembering initial state
    checkFontPackages(settings.getFontPackages(), oldFontPackages);
  }

  // Reset is run when opening settings, clicking cancel, or right after apply.
  @Override
  public void reset() {
    final FlutterSdk sdk = FlutterSdk.getFlutterSdk(myProject);
    resetSdkSelection(sdk);

    final FlutterSettings settings = FlutterSettings.getInstance();
    myHotReloadOnSaveCheckBox.setSelected(settings.isReloadOnSave());
    myFormatCodeOnSaveCheckBox.setSelected(settings.isFormatCodeOnSave());
    myOrganizeImportsOnSaveCheckBox.setSelected(settings.isOrganizeImportsOnSave());

    myShowBuildMethodGuides.setSelected(settings.isShowBuildMethodGuides());
    myShowClosingLabels.setSelected(settings.isShowClosingLabels());

    myShowStructuredErrors.setSelected(settings.isShowStructuredErrors());
    myIncludeAllStackTraces.setSelected(settings.isIncludeAllStackTraces());
    myOpenInspectorOnAppLaunchCheckBox.setSelected(settings.isOpenInspectorOnAppLaunch());
    myEnableLogsPreserveAfterHotReloadOrRestart.setSelected(settings.isPerserveLogsDuringHotReloadAndRestart());
    myEnableVerboseLoggingCheckBox.setSelected(settings.isVerboseLogging());

    myAllowTestsInSourcesRoot.setSelected(settings.isAllowTestsInSourcesRoot());
    myOrganizeImportsOnSaveCheckBox.setEnabled(myFormatCodeOnSaveCheckBox.isSelected());
    myIncludeAllStackTraces.setEnabled(myShowStructuredErrors.isSelected());

    myEnableJcefBrowserCheckBox.setSelected(settings.isEnableJcefBrowser());
    myFontPackagesTextArea.setText(settings.getFontPackages());
    myEnableFilePathLogging.setSelected(settings.isFilePathLoggingEnabled());
  }

  private void resetSdkSelection(@Nullable FlutterSdk sdk) {
    final String path = sdk != null ? sdk.getHomePath() : "";

    try {
      ignoringSdkChanges = true;
      mySdkCombo.getEditor().setItem(FileUtil.toSystemDependentName(path));
      FlutterSdkUtil.addKnownSDKPathsToCombo(mySdkCombo);
    }
    finally {
      ignoringSdkChanges = false;
    }

    // This is to show the version below the SDK selection box.
    onVersionChanged();
    updatePubGetOnSdkChange(sdk);
  }

  private void updatePubGetOnSdkChange(@Nullable FlutterSdk sdk) {
    if (sdk == null) {
      previousSdkVersion = null;
      return;
    }

    if (previousSdkVersion != null && previousSdkVersion.compareTo(sdk.getVersion()) != 0) {
      final List<PubRoot> roots = PubRoots.forProject(myProject);
      OpenApiUtils.safeInvokeLater(() -> {
        for (PubRoot root : roots) {
          sdk.startPubGet(root, myProject);
        }
      });
      previousSdkVersion = sdk.getVersion();
    }
  }

  private void cancelActiveVersionProcess() {
    final ProcessHandler previous = activeVersionProcess.getAndSet(null);
    if (previous != null && !previous.isProcessTerminated()) {
      previous.destroyProcess();
    }
  }

  private void onVersionChanged() {
    mySdkCombo.setEnabled(true);
    cancelActiveVersionProcess();

    final FlutterSdk sdk = FlutterSdk.forPath(getSdkPathText());
    if (sdk == null) {
      // Clear the label out with a non-empty string, so that the layout doesn't give this element 0 height.
      myVersionLabel.setText(" ");
      fullVersionString = null;
      return;
    }

    final ModalityState modalityState = Application.get().getCurrentModalityState();
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      // "flutter --version" can take a long time on a slow network.
      final FlutterCommand command = sdk.flutterVersion();
      final ProcessHandler handler;
      try {
        handler = ProcessHandlerBuilder.create(command.createGeneralCommandLine(null)).colored().build();
      }
      catch (ExecutionException e) {
        LOG.error("Failed to start Flutter command: " + command.getSanitizedDisplayCommand(), e);
        return;
      }
      activeVersionProcess.set(handler);

      final CapturingProcessAdapter listener = new CapturingProcessAdapter();
      handler.addProcessListener(listener);
      handler.startNotify();
      handler.waitFor();

      final ProcessOutput output = listener.getOutput();
      final String stdout = output.getStdout();
      final String[] lines = StringUtil.splitByLines(stdout);
      final String singleLineVersion = lines.length > 0 ? lines[0] : "";

      OpenApiUtils.safeInvokeLater(() -> {
        activeVersionProcess.compareAndSet(handler, null);
        fullVersionString = stdout;
        updateVersionTextIfCurrent(sdk, singleLineVersion);
      }, modalityState);
    });
  }

  @Override
  public void disposeUIResources() {
    cancelActiveVersionProcess();
  }

  /***
   * Sets the version text but only if we don't have stale data.
   *
   * @param sdk the SDK that was current at the time.
   */
  private void updateVersionTextIfCurrent(@NotNull FlutterSdk sdk, @NotNull String value) {
    final FlutterSdk current = FlutterSdk.forPath(getSdkPathText());
    if (current == null) {
      myVersionLabel.setText(" ");
    }
    else {
      myVersionLabel.setText(value);
    }
  }

  @Override
  @NotNull
  public LocalizeValue getDisplayName() {
    return LocalizeValue.of(FLUTTER_SETTINGS_PAGE_NAME);
  }

  @Nullable
  @Override
  public String getHelpTopic() {
    return FLUTTER_SETTINGS_HELP_TOPIC;
  }

  @NotNull
  private String getSdkPathText() {
    final Object item = mySdkCombo.getEditor().getItem();
    return item != null ? FileUtil.toSystemIndependentName(item.toString().trim()) : "";
  }

  private void checkFontPackages(String value, String previous) {
    if (value != null) {
      final String[] packages = value.split(FontPreviewProcessor.PACKAGE_SEPARATORS);
      for (String name : packages) {
        final String message = FontPreviewProcessor.UNSUPPORTED_PACKAGES.get(name.trim());
        if (message != null) {
          displayNotification(message);
        }
      }
      if (!value.equals(previous)) {
        FontPreviewProcessor.reanalyze(myProject);
      }
    }
  }

  private void displayNotification(String message) {
    NotificationService.getInstance().newInfo(FlutterSdkNotificationGroups.FLUTTER_LOGGING)
      .title(FlutterBundle.messageValue("icon.preview.disallow.package.title"))
      .content(LocalizeValue.of(message))
      .icon(FlutterIcons.Flutter)
      .notify(myProject);
  }

  public static void openFlutterSettings(@NotNull final Project project) {
    ShowSettingsUtil.getInstance().showSettingsDialog(project, FlutterConstants.FLUTTER_SETTINGS_PAGE_ID, "");
  }
}
