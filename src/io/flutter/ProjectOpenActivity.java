/*
 * Copyright 2017 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter;

import com.intellij.framework.FrameworkType;
import com.intellij.framework.detection.DetectionExcludesConfiguration;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import consulo.ide.impl.idea.ide.plugins.PluginManagerCore;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationType;
import consulo.project.ui.notification.Notifications;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.application.ApplicationManager;
import consulo.container.plugin.PluginId;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.project.ProjectType;
import com.intellij.openapi.project.ProjectTypeService;
import consulo.ui.ex.awt.Messages;
import io.flutter.icons.FlutterIcons;
import io.flutter.jxbrowser.JxBrowserManager;
import io.flutter.pub.PubRoot;
import io.flutter.pub.PubRoots;
import io.flutter.sdk.FlutterSdk;
import io.flutter.utils.AndroidUtils;
import io.flutter.utils.FlutterModuleUtils;
import org.jetbrains.android.facet.AndroidFrameworkDetector;
import org.jetbrains.annotations.NotNull;
import java.util.Collection;

/**
 * Runs startup actions just after a project is opened, before it's indexed.
 *
 * @see FlutterInitializer for actions that run later.
 */
public class ProjectOpenActivity extends FlutterProjectActivity implements DumbAware {
  public static final ProjectType FLUTTER_PROJECT_TYPE = new ProjectType("io.flutter");

  public ProjectOpenActivity() {
  }

  @Override
  public void executeProjectStartup(@NotNull Project project) {
    if (!FlutterModuleUtils.declaresFlutter(project)) {
      return;
    }

    // Set up JxBrowser listening and check if it's already enabled.
    JxBrowserManager.getInstance().listenForSettingChanges(project);
    JxBrowserManager.getInstance().setUp(project.getName());
    excludeAndroidFrameworkDetector(project);

    final FlutterSdk sdk = FlutterSdk.getIncomplete(project);
    if (sdk == null) {
      // We can't do anything without a Flutter SDK.
      // Note: this branch is taken when opening a project immediately after creating it -- not sure that is expected.
      return;
    }

    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      sdk.queryFlutterConfig("android-studio-dir", false);
    });
    Collection<ProjectType> projectTypes = ProjectTypeService.getProjectTypes(project);
    for (ProjectType projectType : projectTypes) {
      if (projectType != null && FlutterUtils.isAndroidStudio() && !FLUTTER_PROJECT_TYPE.equals(projectType)) {
        if (!AndroidUtils.isAndroidProject(project)) {
          ProjectTypeService.setProjectType(project, FLUTTER_PROJECT_TYPE);
        }
      }
    }

    for (PubRoot pubRoot : PubRoots.forProject(project)) {
      if (!pubRoot.hasUpToDatePackages()) {
        Notifications.Bus.notify(new PackagesOutOfDateNotification(project, pubRoot), project);
      }
    }
  }

  private static void excludeAndroidFrameworkDetector(@NotNull Project project) {
    if (FlutterUtils.isAndroidStudio()) {
      return;
    }
    IdeaPluginDescriptor desc;
    if (PluginManagerCore.getPlugin(PluginId.getId("org.jetbrains.android")) == null) {
      return;
    }
    try {
      final DetectionExcludesConfiguration excludesConfiguration = DetectionExcludesConfiguration.getInstance(project);
      try {
        final FrameworkType type = new AndroidFrameworkDetector().getFrameworkType();
        if (!excludesConfiguration.isExcludedFromDetection(type)) {
          excludesConfiguration.addExcludedFramework(type);
        }
      }
      catch (NullPointerException ignored) {
        // If the Android facet has not been configured then getFrameworkType() throws a NPE.
      }
    }
    catch (NoClassDefFoundError ignored) {
      // This should never happen. But just in case ...
    }
  }

  private static class PackagesOutOfDateNotification extends Notification {
    public PackagesOutOfDateNotification(@NotNull Project project, @NotNull PubRoot root) {
      super("Flutter Packages", "The pubspec.yaml file has been modified since " +
                                "the last time 'flutter pub get' was run.", NotificationType.INFORMATION);
      setIcon(FlutterIcons.Flutter);

      //noinspection DialogTitleCapitalization
      addAction(new AnAction("Run 'flutter pub get'") {
        @Override
        public void actionPerformed(@NotNull AnActionEvent event) {
          expire();

          final FlutterSdk sdk = FlutterSdk.getFlutterSdk(project);
          if (sdk == null) {
            Messages.showErrorDialog(project, "Flutter SDK not found", "Error");
            return;
          }

          if (sdk.startPubGet(root, project) == null) {
            Messages.showErrorDialog("Unable to run 'flutter pub get'", "Error");
          }
        }
      });
    }
  }
}
