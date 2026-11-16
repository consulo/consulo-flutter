/*
 * Copyright 2016 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter;

import consulo.flutter.notification.FlutterNotificationGroups;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationService;
import consulo.ui.ex.awt.Messages;
import io.flutter.icons.FlutterIcons;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FlutterMessages {
  public static final String FLUTTER_NOTIFICATION_GROUP_ID = "Flutter Messages";
  public static final String FLUTTER_LOGGING_NOTIFICATION_GROUP_ID = "Flutter Notifications";

  private FlutterMessages() {
  }

  public static void showError(@NotNull String title, @NotNull String message, @Nullable Project project) {
    NotificationService.getInstance()
      .newError(FlutterNotificationGroups.FLUTTER_MESSAGES)
      .title(LocalizeValue.of(title))
      .content(LocalizeValue.of(message))
      .notify(project);
  }

  public static void showWarning(@NotNull String title, @NotNull String message, @Nullable Project project) {
    NotificationService.getInstance()
      .newWarn(FlutterNotificationGroups.FLUTTER_MESSAGES)
      .title(LocalizeValue.of(title))
      .content(LocalizeValue.of(message))
      .notify(project);
  }

  public static void showInfo(@NotNull String title, @NotNull String message, @Nullable Project project) {
    NotificationService.getInstance()
      .newInfo(FlutterNotificationGroups.FLUTTER_MESSAGES)
      .title(LocalizeValue.of(title))
      .content(LocalizeValue.of(message))
      .icon(FlutterIcons.Flutter)
      .notify(project);
  }

  public static int showDialog(@Nullable Project project,
                               @NotNull String message,
                               @NotNull @Nls String title,
                               @NotNull String[] options,
                               int defaultOptionIndex) {
    // Consulo: Messages.showDialog instead of MessagesService
    return Messages.showDialog(project, message, title, options, defaultOptionIndex, FlutterIcons.Flutter_2x);
  }
}
