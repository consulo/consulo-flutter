/*
 * Copyright 2017 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter;

import consulo.annotation.component.ExtensionImpl;

import consulo.application.ApplicationManager;
import consulo.application.dumb.DumbAware;
import consulo.flutter.notification.FlutterNotificationGroups;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationService;
import consulo.ui.ex.awt.Messages;
import io.flutter.icons.FlutterIcons;
import io.flutter.pub.PubRoot;
import io.flutter.pub.PubRoots;
import io.flutter.sdk.FlutterSdk;
import io.flutter.utils.FlutterModuleUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Runs startup actions just after a project is opened, before it's indexed.
 *
 * @see FlutterInitializer for actions that run later.
 */
@ExtensionImpl
public class ProjectOpenActivity extends FlutterProjectActivity implements DumbAware {
  // Consulo: the Android Studio project type (FLUTTER_PROJECT_TYPE) is dropped.

  public ProjectOpenActivity() {
  }

  @Override
  public void executeProjectStartup(@NotNull Project project) {
    if (!FlutterModuleUtils.declaresFlutter(project)) {
      return;
    }

    // Consulo: JxBrowser is dropped, and so is the Android framework detector workaround (excludeAndroidFrameworkDetector).

    final FlutterSdk sdk = FlutterSdk.getIncomplete(project);
    if (sdk == null) {
      // We can't do anything without a Flutter SDK.
      // Note: this branch is taken when opening a project immediately after creating it -- not sure that is expected.
      return;
    }

    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      sdk.queryFlutterConfig("android-studio-dir", false);
    });
    // Consulo: setting the Android Studio project type is dropped (Android Studio only).

    for (PubRoot pubRoot : PubRoots.forProject(project)) {
      if (!pubRoot.hasUpToDatePackages()) {
        notifyPackagesOutOfDate(project, pubRoot);
      }
    }
  }

  // Consulo: upstream PackagesOutOfDateNotification subclass -> notification builder
  private static void notifyPackagesOutOfDate(@NotNull Project project, @NotNull PubRoot root) {
    NotificationService.getInstance()
      .newInfo(FlutterNotificationGroups.FLUTTER_PACKAGES)
      .content(LocalizeValue.localizeTODO("The pubspec.yaml file has been modified since " +
                                          "the last time 'flutter pub get' was run."))
      .icon(FlutterIcons.Flutter)
      //noinspection DialogTitleCapitalization
      .addClosingAction(LocalizeValue.localizeTODO("Run 'flutter pub get'"), () -> {
        final FlutterSdk sdk = FlutterSdk.getFlutterSdk(project);
        if (sdk == null) {
          Messages.showErrorDialog(project, "Flutter SDK not found", "Error");
          return;
        }

        if (sdk.startPubGet(root, project) == null) {
          Messages.showErrorDialog("Unable to run 'flutter pub get'", "Error");
        }
      })
      .notify(project);
  }
}
