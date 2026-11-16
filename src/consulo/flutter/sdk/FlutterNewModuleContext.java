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

import consulo.module.creation.NewModuleWizardContextBase;
import io.flutter.sdk.FlutterCreateAdditionalSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FlutterNewModuleContext extends NewModuleWizardContextBase {
  private @Nullable String myFlutterSdkPath;
  private @NotNull FlutterCreateAdditionalSettings myAdditionalSettings = new FlutterCreateAdditionalSettings();

  public FlutterNewModuleContext(boolean isNewProject) {
    super(isNewProject);
  }

  public @Nullable String getFlutterSdkPath() {
    return myFlutterSdkPath;
  }

  public void setFlutterSdkPath(@Nullable String flutterSdkPath) {
    myFlutterSdkPath = flutterSdkPath;
  }

  /**
   * @return the {@code flutter create} options chosen in the wizard
   */
  public @NotNull FlutterCreateAdditionalSettings getAdditionalSettings() {
    return myAdditionalSettings;
  }

  public void setAdditionalSettings(@NotNull FlutterCreateAdditionalSettings additionalSettings) {
    myAdditionalSettings = additionalSettings;
  }
}
