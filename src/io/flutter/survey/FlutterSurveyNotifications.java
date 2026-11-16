/*
 * Copyright 2019 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.survey;

import consulo.webBrowser.BrowserUtil;
import consulo.application.ApplicationPropertiesComponent;
import consulo.component.PropertiesComponent;
import consulo.application.Application;
import consulo.flutter.notification.FlutterNotificationGroups;
import consulo.localize.LocalizeValue;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationService;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.event.FileEditorManagerEvent;
import consulo.fileEditor.event.FileEditorManagerListener;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import com.jetbrains.lang.dart.sdk.DartSdk;
import io.flutter.icons.FlutterIcons;
import io.flutter.FlutterUtils;
import io.flutter.pub.PubRoot;
import io.flutter.sdk.FlutterSdk;
import io.flutter.sdk.FlutterSdkVersion;
import org.jetbrains.annotations.NotNull;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FlutterSurveyNotifications {
  private static final int NOTIFICATION_DELAY_IN_SECS = 3;

  private static final String FLUTTER_LAST_SURVEY_PROMPT_KEY = "FLUTTER_LAST_SURVEY_PROMPT_KEY";
  private static final long PROMPT_INTERVAL_IN_MS = TimeUnit.HOURS.toMillis(40);

  private static final String SURVEY_ACTION_TEXT = "Take survey";
  private static final String SURVEY_DISMISSAL_TEXT = "No thanks";

  interface FlutterSurveyNotifier {
    void prompt();
  }

  @NotNull final Project myProject;

  FlutterSurveyNotifications(@NotNull Project project) {
    this.myProject = project;
  }

  public static void init(@NotNull Project project) {
    project.getMessageBus().connect().subscribe(FileEditorManagerListener.class, new FileEditorManagerListener() {
      @Override
      public void fileOpened(@NotNull final FileEditorManager source, @NotNull final VirtualFile file) {
        check(file);
      }

      @Override
      public void selectionChanged(@NotNull FileEditorManagerEvent event) {
        final VirtualFile file = event.getNewFile();
        if (file != null) {
          check(file);
        }
      }

      private void check(@NotNull VirtualFile file) {
        if (PubRoot.isPubspec(file) || FlutterUtils.isDartFile(file)) {
          new FlutterSurveyNotifications(project).checkForDisplaySurvey();
        }
      }
    });
  }

  private void checkForDisplaySurvey() {
    final FlutterSurvey survey = FlutterSurveyService.getLatestSurveyContent();
    if (survey == null) return;

    // Limit display to the survey window.
    if (!survey.isSurveyOpen()) return;

    final PropertiesComponent properties = ApplicationPropertiesComponent.getInstance();

    // Don't prompt more often than every 40 hours.
    final long lastPromptedMillis = properties.getLong(FLUTTER_LAST_SURVEY_PROMPT_KEY, 0);
    if (System.currentTimeMillis() - lastPromptedMillis < PROMPT_INTERVAL_IN_MS) return;

    properties.setValue(FLUTTER_LAST_SURVEY_PROMPT_KEY, String.valueOf(System.currentTimeMillis()));

    // Or, if the survey has already been taken.
    if (properties.getBoolean(survey.uniqueId)) return;

    // Consulo: notification builder instead of Notification subclassing/AnActions
    final Notification.Builder notification = NotificationService.getInstance()
      .newInfo(FlutterNotificationGroups.FLUTTER_MESSAGES)
      .title(LocalizeValue.of(survey.title))
      .icon(FlutterIcons.Flutter);

    notification.addClosingAction(LocalizeValue.localizeTODO(SURVEY_ACTION_TEXT), () -> {
      properties.setValue(survey.uniqueId, true);

      // Consulo: the application name instead of ApplicationInfo.getVersionName()
      String rawIdeName = Application.get().getName().get();
      if (rawIdeName.isEmpty()) {
        rawIdeName = "IntelliJ - Unknown";
      }
      final String ideName = URLEncoder.encode(rawIdeName, StandardCharsets.UTF_8);
      StringBuilder stringBuilder = new StringBuilder(survey.urlPrefix + "?Source=" + ideName);

      final DartSdk dartSdk = DartSdk.getDartSdk(myProject);
      if (dartSdk != null) {
        stringBuilder.append("&DartVersion=").append(dartSdk.getVersion());
      }

      final FlutterSdk flutterSdk = FlutterSdk.getFlutterSdk(myProject);
      final FlutterSdkVersion flutterSdkVersion = flutterSdk == null ? null : flutterSdk.getVersion();
      if (flutterSdkVersion != null) {
        stringBuilder.append("&FlutterVersion=").append(flutterSdkVersion.getVersionText());
      }

      String url = stringBuilder.toString();
      BrowserUtil.browse(url);
    });

    notification.addClosingAction(LocalizeValue.localizeTODO(SURVEY_DISMISSAL_TEXT), () -> {
      properties.setValue(survey.uniqueId, true);
    });

    // Display the prompt after a short delay.
    try (var scheduler = Executors.newSingleThreadScheduledExecutor()) {
      scheduler.schedule(() -> {
        if (!myProject.isDisposed()) {
          notification.notify(myProject);
        }
      }, NOTIFICATION_DELAY_IN_SECS, TimeUnit.SECONDS);
      scheduler.shutdown();
    }
  }
}
