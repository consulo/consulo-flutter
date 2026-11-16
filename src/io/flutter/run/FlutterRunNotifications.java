/*
 * Copyright 2017 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.run;

import consulo.webBrowser.BrowserUtil;
import consulo.component.PropertiesComponent;
import consulo.project.ProjectPropertiesComponent;
import consulo.flutter.notification.FlutterRunNotificationGroups;
import consulo.project.ui.notification.NotificationService;
import consulo.application.ApplicationManager;
import consulo.project.Project;
import io.flutter.icons.FlutterIcons;
import io.flutter.FlutterBundle;
import consulo.localize.LocalizeValue;
import io.flutter.view.FlutterViewMessages;
import org.jetbrains.annotations.NotNull;

public class FlutterRunNotifications {
  private static final String RELOAD_ALREADY_RUN = "io.flutter.reload.alreadyRun";

  public static void init(@NotNull Project project) {
    // Initialize the flutter run notification group.
    // Consulo: notification groups are registered by NotificationGroupContributor

    final FlutterRunNotifications notifications = new FlutterRunNotifications(project);

    //noinspection CodeBlock2Expr
    project.getMessageBus().connect().subscribe(
      FlutterViewMessages.FlutterDebugNotifier.class, (event) -> { // Consulo: topics are listener classes
        ApplicationManager.getApplication().invokeLater(notifications::checkForDisplayFirstReload);
      }
    );
  }

  @NotNull final Project myProject;

  FlutterRunNotifications(@NotNull Project project) {
    this.myProject = project;
  }

  private void checkForDisplayFirstReload() {
    if (myProject.isDisposed()) {
      return;
    }
    final PropertiesComponent properties = ProjectPropertiesComponent.getInstance(myProject);

    final boolean alreadyRun = properties.getBoolean(RELOAD_ALREADY_RUN);
    if (!alreadyRun) {
      properties.setValue(RELOAD_ALREADY_RUN, true);

      NotificationService.getInstance()
        .newInfo(FlutterRunNotificationGroups.FLUTTER_MESSAGES)
        .title(FlutterBundle.messageValue("flutter.reload.firstRun.title"))
        .content(FlutterBundle.messageValue("flutter.reload.firstRun.content"))
        .icon(FlutterIcons.HotReload)
        .addClosingAction(LocalizeValue.localizeTODO("Learn More"),
                          () -> BrowserUtil.browse(FlutterBundle.message("flutter.reload.firstRun.url")))
        .notify(myProject);
    }
  }
}
