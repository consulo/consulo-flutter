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

import com.jetbrains.lang.dart.projectWizard.DartModuleBuilder;
import com.jetbrains.lang.dart.sdk.DartSdkLibUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.module.content.layer.ContentEntry;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.creation.scratch.NewModuleBuilderProcessor;
import consulo.process.util.CapturingProcessAdapter;
import consulo.project.Project;
import consulo.ui.ex.wizard.WizardStep;
import consulo.virtualFileSystem.VirtualFile;
import io.flutter.FlutterMessages;
import io.flutter.actions.FlutterDoctorAction;
import io.flutter.module.FlutterModuleBuilder;
import io.flutter.pub.PubRoot;
import io.flutter.sdk.FlutterCreateAdditionalSettings;
import io.flutter.sdk.FlutterSdk;
import io.flutter.sdk.FlutterSdkUtil;
import io.flutter.utils.FlutterModuleUtils;
import io.flutter.utils.OpenApiUtils;

import java.util.function.Consumer;

/**
 * Upstream {@code FlutterModuleBuilder.setupRootModel} / {@code commitModule}: enables Dart with the Dart SDK of the Flutter SDK
 * and runs {@code flutter create} in the content root.
 */
public class FlutterNewModuleBuilderProcessor implements NewModuleBuilderProcessor<FlutterNewModuleContext> {
  @Override
  public FlutterNewModuleContext createContext(boolean isNewProject) {
    return new FlutterNewModuleContext(isNewProject);
  }

  @Override
  public void buildSteps(Consumer<WizardStep<FlutterNewModuleContext>> consumer, FlutterNewModuleContext context) {
    consumer.accept(new FlutterNewModuleSetupStep(context));
  }

  @RequiredReadAction
  @Override
  public void process(FlutterNewModuleContext context, ContentEntry contentEntry, ModifiableRootModel modifiableRootModel) {
    final Project project = modifiableRootModel.getProject();
    final String sdkPath = context.getFlutterSdkPath();
    final FlutterSdk sdk = sdkPath == null ? null : FlutterSdk.forPath(sdkPath);
    final String dartSdkPath = sdk == null ? null : FlutterSdkUtil.pathToDartSdk(sdk.getHomePath());

    // Consulo: upstream adds a reference to the "Dart SDK" project library; here the Dart SDK of the Flutter SDK
    // is a Consulo SDK selected in the Dart module extension
    if (dartSdkPath != null) {
      Application.get().runWriteAction(() -> DartSdkLibUtil.ensureDartSdkConfigured(project, dartSdkPath));
      DartModuleBuilder.enableDart(modifiableRootModel, DartSdkLibUtil.findDartSdkByHomePath(dartSdkPath));
    }
    else {
      DartModuleBuilder.enableDart(modifiableRootModel, null);
    }

    final VirtualFile baseDir = contentEntry.getFile();
    if (baseDir == null || sdk == null) {
      return;
    }

    final FlutterCreateAdditionalSettings settings = context.getAdditionalSettings();
    settings.setProjectName(context.getName());

    // Consulo: the module is committed after process(), so flutter create runs in the background
    // instead of upstream's modal progress (FlutterModuleBuilder.runFlutterCreateWithProgress) before the commit
    Application.get().executeOnPooledThread(() -> {
      final CapturingProcessAdapter listener = new CapturingProcessAdapter();
      final PubRoot root = sdk.createFiles(baseDir, null, listener, settings);
      if (root == null) {
        OpenApiUtils.safeInvokeLater(() -> {
          final String stderr = listener.getOutput().getStderr();
          final String msg = stderr.isEmpty() ? "Flutter create command was unsuccessful" : stderr;
          final int code =
            FlutterMessages.showDialog(project, msg, "Project Creation Error", new String[]{"Run Flutter Doctor", "Cancel"}, 0);
          if (code == 0) {
            new FlutterDoctorAction().startCommand(project, sdk, null);
          }
        });
        return;
      }
      FlutterSdkUtil.updateKnownSdkPaths(sdk.getHomePath());

      FlutterModuleUtils.autoShowMain(project, root);
      FlutterModuleBuilder.showProjectInProjectWindow(project);
    });
  }
}
