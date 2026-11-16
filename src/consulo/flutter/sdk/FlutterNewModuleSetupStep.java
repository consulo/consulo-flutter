/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.flutter.sdk;

import consulo.configurable.ConfigurationException;
import consulo.disposer.Disposable;
import consulo.module.creation.ui.UnifiedProjectOrModuleNameStep;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.wizard.WizardStepValidationException;
import consulo.ui.util.FormBuilder;
import consulo.virtualFileSystem.LocalFileSystem;
import io.flutter.FlutterUtils;
import io.flutter.module.FlutterGeneratorPeer;
import io.flutter.module.FlutterModuleBuilder;
import io.flutter.module.settings.FlutterCreateAdditionalSettingsFields;
import io.flutter.sdk.FlutterCreateAdditionalSettings;
import io.flutter.sdk.FlutterSdk;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Flutter SDK and {@code flutter create} options, as upstream {@code FlutterModuleBuilder.FlutterModuleWizardStep} (the SDK page,
 * {@link FlutterGeneratorPeer}) and {@code FlutterModuleBuilder.modifySettingsStep} (the settings fields) on one page.
 */
public class FlutterNewModuleSetupStep extends UnifiedProjectOrModuleNameStep<FlutterNewModuleContext> {
  private @Nullable FlutterGeneratorPeer myPeer;
  private @Nullable FlutterCreateAdditionalSettingsFields mySettingsFields;

  public FlutterNewModuleSetupStep(FlutterNewModuleContext context) {
    super(context);
  }

  @RequiredUIAccess
  @Override
  protected void extend(FormBuilder builder, Disposable uiDisposable) {
    super.extend(builder, uiDisposable);

    final FlutterGeneratorPeer peer = new FlutterGeneratorPeer();
    if (!FlutterUtils.isAndroidStudio()) {
      peer.getHelpForm().showGettingStarted();
    }
    builder.addBottom(TargetAWT.wrap(peer.getComponent()));
    myPeer = peer;

    final FlutterCreateAdditionalSettingsFields settingsFields =
      new FlutterCreateAdditionalSettingsFields(new FlutterCreateAdditionalSettings(), this::getFlutterSdk);
    // upstream does this when leaving the SDK page, before the settings page is shown
    settingsFields.updateProjectTypes();
    settingsFields.addSettingsFields(builder);
    mySettingsFields = settingsFields;
  }

  @RequiredUIAccess
  @Override
  public CompletableFuture<?> validateStep(FlutterNewModuleContext context) {
    final FlutterGeneratorPeer peer = myPeer;
    final FlutterCreateAdditionalSettingsFields settingsFields = mySettingsFields;
    if (peer == null || settingsFields == null) {
      return CompletableFuture.completedFuture(null);
    }

    // FlutterModuleWizardStep.validate()
    if (!peer.validate()) {
      return CompletableFuture.failedFuture(new WizardStepValidationException(peer.getComponent(), "Invalid Flutter SDK path"));
    }

    // FlutterModuleBuilder.validateModuleName()
    try {
      FlutterModuleBuilder.validateModuleName(getName());
    }
    catch (ConfigurationException e) {
      return CompletableFuture.failedFuture(new WizardStepValidationException(e.getMessage()));
    }

    // FlutterModuleBuilder.validate()
    final String settingsValidation =
      FlutterModuleBuilder.validateSettings(settingsFields.getAdditionalSettings(), settingsFields.shouldIncludePlatforms());
    if (settingsValidation != null) {
      return CompletableFuture.failedFuture(new WizardStepValidationException(settingsValidation));
    }
    if (getFlutterSdk() == null) {
      return CompletableFuture.failedFuture(new WizardStepValidationException(peer.getComponent(), "Flutter SDK not found"));
    }
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public void onStepLeave(FlutterNewModuleContext context) {
    super.onStepLeave(context);

    if (myPeer != null) {
      context.setFlutterSdkPath(myPeer.getSdkComboPath());
    }
    if (mySettingsFields != null) {
      context.setAdditionalSettings(mySettingsFields.getAdditionalSettings());
    }
  }

  // FlutterModuleWizardStep.getFlutterSdk()
  @Nullable
  private FlutterSdk getFlutterSdk() {
    if (myPeer == null) {
      return null;
    }
    final String sdkPath = myPeer.getSdkComboPath();

    // Ensure the local filesystem has caught up to external processes (e.g., git clone).
    if (!sdkPath.isEmpty()) {
      try {
        LocalFileSystem
          .getInstance().refreshAndFindFileByPath(sdkPath);
      }
      catch (Throwable e) {
        // It's possible that the refresh will fail in which case we just want to trap and ignore.
      }
    }
    return FlutterSdk.forPath(sdkPath);
  }
}
