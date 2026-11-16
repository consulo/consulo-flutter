/*
 * Copyright 2017 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.run;

import consulo.webBrowser.BrowserUtil;
import consulo.component.PropertiesComponent;
import com.intellij.notification.*;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.application.ApplicationManager;
import consulo.project.Project;
import io.flutter.icons.FlutterIcons;
import io.flutter.FlutterBundle;
import io.flutter.FlutterMessages;
import io.flutter.view.FlutterViewMessages;
import org.jetbrains.annotations.NotNull;

public class FlutterRunNotifications {
  private static final String RELOAD_ALREADY_RUN = "io.flutter.reload.alreadyRun";

  public static void init(@NotNull Project project) {
    // Initialize the flutter run notification group.
    NotificationsConfiguration.getNotificationsConfiguration().register(
      FlutterMessages.FLUTTER_NOTIFICATION_GROUP_ID,
      NotificationDisplayType.BALLOON,
      false);
    NotificationsConfiguration.getNotificationsConfiguration().register(
      FlutterMessages.FLUTTER_LOGGING_NOTIFICATION_GROUP_ID,
      NotificationDisplayType.BALLOON,
      true);

    final FlutterRunNotifications notifications = new FlutterRunNotifications(project);

    //noinspection CodeBlock2Expr
    project.getMessageBus().connect().subscribe(
      FlutterViewMessages.FLUTTER_DEBUG_TOPIC, (event) -> {
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
    final PropertiesComponent properties = PropertiesComponent.getInstance(myProject);

    final boolean alreadyRun = properties.getBoolean(RELOAD_ALREADY_RUN);
    if (!alreadyRun) {
      properties.setValue(RELOAD_ALREADY_RUN, true);

      final Notification notification = new Notification(
        FlutterMessages.FLUTTER_NOTIFICATION_GROUP_ID,
        FlutterBundle.message("flutter.reload.firstRun.title"),
        FlutterBundle.message("flutter.reload.firstRun.content"),
        NotificationType.INFORMATION);
      notification.setIcon(FlutterIcons.HotReload);
      notification.addAction(new AnAction("Learn More") {
        @Override
        public void actionPerformed(@NotNull AnActionEvent event) {
          BrowserUtil.browse(FlutterBundle.message("flutter.reload.firstRun.url"));
          notification.expire();
        }
      });
      Notifications.Bus.notify(notification, myProject);
    }
  }
}
