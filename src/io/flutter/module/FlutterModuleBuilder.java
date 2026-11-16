/*
 * Copyright 2016 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.module;

import consulo.application.progress.ProgressManager;
import consulo.configurable.ConfigurationException;
import consulo.ide.impl.idea.ide.projectView.impl.ProjectViewPaneImpl;
import consulo.logging.Logger;
import consulo.module.ModifiableModuleModel;
import consulo.process.event.ProcessListener;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ui.view.ProjectView;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import io.flutter.FlutterBundle;
import io.flutter.FlutterConstants;
import io.flutter.FlutterUtils;
import io.flutter.icons.FlutterIcons;
import io.flutter.logging.PluginLogger;
import io.flutter.pub.PubRoot;
import io.flutter.sdk.FlutterCreateAdditionalSettings;
import io.flutter.sdk.FlutterSdk;
import io.flutter.utils.AndroidUtils;
import io.flutter.utils.OpenApiUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

import static java.util.Arrays.asList;

/**
 * Consulo: the IntelliJ ModuleBuilder / ModuleWizardStep / SettingsStep wizard does not exist. The new project wizard is
 * {@code consulo.flutter.sdk.FlutterNewModuleBuilder} (with its processor and setup step); this class keeps the upstream
 * logic it shares (names, validation, {@code flutter create} with progress). Wizard overrides of upstream are dropped:
 * {@code setupRootModel}, {@code commitModule} (moved to the processor), {@code modifySettingsStep}, {@code modifyProjectTypeStep},
 * {@code getCustomOptionsStep}, {@code getBuilderId}, {@code getModuleType} and the {@code FlutterModuleWizardStep} inner class
 * (moved to the setup step).
 */
public class FlutterModuleBuilder {
  private static final @NotNull Logger LOG = PluginLogger.createLogger(FlutterModuleBuilder.class);

  private FlutterModuleBuilder() {
  }

  public static String getPresentableName() {
    return FlutterBundle.message("flutter.module.name");
  }

  public static String getDescription() {
    return FlutterBundle.message("flutter.project.description");
  }

  public static Image getNodeIcon() {
    return FlutterIcons.Flutter;
  }

  public static void showProjectInProjectWindow(@NotNull Project project) {
    OpenApiUtils.safeInvokeLater(() -> {
      DumbService.getInstance(project).runWhenSmart(() -> {
        OpenApiUtils.safeInvokeLater(() -> {
          ProjectView view = ProjectView.getInstance(project);
          if (view == null) return;
          view.changeView(ProjectViewPaneImpl.ID);
        });
      });
    });
  }

  // Consulo: static, the platform selection check comes from the settings fields of the wizard step
  @Nullable
  public static String validateSettings(@NotNull FlutterCreateAdditionalSettings settings, boolean shouldIncludePlatforms) {
    final String description = settings.getDescription();
    if (description != null && description.contains(": ")) {
      return FlutterBundle.message("npw_invalid_desc_error", description);
    }
    final String org = settings.getOrg();
    if (org == null) {
      return null;
    }
    if (StringUtil.endsWith(org, ".")) {
      return FlutterBundle.message("npw_invalid_org_error", org);
    }
    if (shouldIncludePlatforms && !settings.isSomePlatformSelected()) {
      return FlutterBundle.message("npw_none_selected_error");
    }
    // Invalid package names will cause issues down the line.
    return AndroidUtils.validateAndroidPackageName(org);
  }

  // Consulo: Android Studio integration (loading the generated android/*.iml modules) is dropped, see CONSULO.md
  public static void addAndroidModule(@NotNull Project project,
                                      @Nullable ModifiableModuleModel model,
                                      @NotNull String baseDirPath,
                                      @NotNull String flutterModuleName,
                                      boolean isTopLevel) {
    final VirtualFile baseDir = LocalFileSystem.getInstance().refreshAndFindFileByPath(baseDirPath);
    if (baseDir == null) {
      return;
    }

    final VirtualFile androidFile = isTopLevel
                                    ? findAndroidModuleFile(baseDir, flutterModuleName)
                                    : findEmbeddedModuleFile(baseDir, flutterModuleName);
    if (androidFile == null) return;
    addAndroidModuleFromFile(project, model, androidFile);
  }

  public static void addAndroidModuleFromFile(@NotNull Project project,
                                              @Nullable ModifiableModuleModel model,
                                              @NotNull VirtualFile androidFile) {
    // Consulo: TODO modules are not loaded from IntelliJ .iml files (ModifiableModuleModel.loadModule does not exist)
    LOG.info("Android module is not added: " + androidFile.getPath());
  }

  @Nullable
  private static VirtualFile findAndroidModuleFile(@NotNull VirtualFile baseDir, String flutterModuleName) {
    baseDir.refresh(false, false);
    for (String name : asList(flutterModuleName + "_android.iml", "android.iml")) {
      final VirtualFile candidate = baseDir.findChild(name);
      if (candidate != null && candidate.exists()) {
        return candidate;
      }
    }
    return null;
  }

  @Nullable
  private static VirtualFile findEmbeddedModuleFile(@NotNull VirtualFile baseDir, String flutterModuleName) {
    baseDir.refresh(false, false);
    for (String name : asList("android", ".android")) {
      VirtualFile dir = baseDir.findChild(name);
      if (dir != null && dir.exists()) {
        VirtualFile candidate = dir.findChild(flutterModuleName + "_android.iml");
        if (candidate != null && candidate.exists()) {
          return candidate;
        }
      }
    }
    return null;
  }

  /**
   * @see <a href="dart.dev/tools/pub/pubspec#name">https://dart.dev/tools/pub/pubspec#name</a>
   */
  public static boolean validateModuleName(@NotNull String moduleName) throws ConfigurationException {
    if (!FlutterUtils.isValidPackageName(moduleName)) {
      throw new ConfigurationException(
        "Invalid module name: '" + moduleName + "' - must be a valid Dart package name (lower_case_with_underscores).");
    }

    if (FlutterUtils.isDartKeyword(moduleName)) {
      throw new ConfigurationException("Invalid module name: '" + moduleName + "' - must not be a Dart keyword.");
    }

    if (!FlutterUtils.isValidDartIdentifier(moduleName)) {
      throw new ConfigurationException("Invalid module name: '" + moduleName + "' - must be a valid Dart identifier.");
    }

    if (FlutterConstants.FLUTTER_PACKAGE_DEPENDENCIES.contains(moduleName)) {
      throw new ConfigurationException(
        "Invalid module name: '" + moduleName + "' - this will conflict with Flutter package dependencies.");
    }

    return true;
  }

  /**
   * Runs flutter create without showing a console, but with an indeterminate progress dialog.
   * <p>
   * Returns the PubRoot if successful.
   */
  @Nullable
  public static PubRoot runFlutterCreateWithProgress(@NotNull VirtualFile baseDir,
                                                     @NotNull FlutterSdk sdk,
                                                     @NotNull Project project,
                                                     @Nullable ProcessListener processListener,
                                                     @Nullable FlutterCreateAdditionalSettings additionalSettings) {
    final ProgressManager progress = ProgressManager.getInstance();
    final AtomicReference<PubRoot> result = new AtomicReference<>(null);

    FlutterUtils.disableGradleProjectMigrationNotification(project);

    //noinspection DialogTitleCapitalization
    progress.runProcessWithProgressSynchronously(() -> {
      progress.getProgressIndicator().setIndeterminate(true);
      result.set(sdk.createFiles(baseDir, null, processListener, additionalSettings));
    }, "Creating Flutter project", false, project);

    return result.get();
  }
}
