/*
 * Copyright 2016 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.inspections;

import consulo.annotation.component.ExtensionImpl;

import com.jetbrains.lang.dart.DartLanguage;
import consulo.annotation.access.RequiredReadAction;
import consulo.fileEditor.EditorNotificationBuilder;
import consulo.fileEditor.EditorNotificationProvider;
import consulo.fileEditor.EditorNotifications;
import consulo.fileEditor.FileEditor;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.util.ModuleUtilCore;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import io.flutter.FlutterBundle;
import io.flutter.FlutterUtils;
import io.flutter.icons.FlutterIcons;
import io.flutter.sdk.FlutterSdk;
import io.flutter.utils.FlutterModuleUtils;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

@ExtensionImpl
public class SdkConfigurationNotificationProvider implements EditorNotificationProvider {
  // Consulo: upstream hides the panel on "Dismiss"; here the dismissal is remembered on the file.
  private static final Key<Boolean> DISMISSED = Key.create("flutter.no.sdk.notification.dismissed");

  @NotNull
  private final Project project;

  @Inject
  public SdkConfigurationNotificationProvider(@NotNull Project project) {
    this.project = project;
  }

  @NotNull
  @Override
  public String getId() {
    return "flutter.sdk.configuration.notification";
  }

  // Consulo: EditorNotificationProvider fills an EditorNotificationBuilder instead of returning a panel factory
  @RequiredReadAction
  @Nullable
  @Override
  public EditorNotificationBuilder buildNotification(@NotNull VirtualFile file,
                                                     @NotNull FileEditor fileEditor,
                                                     @NotNull Supplier<EditorNotificationBuilder> builderFactory) {
    if (!FlutterUtils.isDartFile(file)) return null;
    if (Boolean.TRUE.equals(file.getUserData(DISMISSED))) return null;

    final PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
    if (psiFile == null || psiFile.getLanguage() != DartLanguage.INSTANCE) return null;

    final Module module = ModuleUtilCore.findModuleForPsiElement(psiFile);
    if (!FlutterModuleUtils.isFlutterModule(module)) return null;

    final FlutterSdk flutterSdk = FlutterSdk.getFlutterSdk(project);
    if (flutterSdk == null) {
      return createNoFlutterSdkPanel(builderFactory.get(), file, project);
    }
    return null;
  }

  private static EditorNotificationBuilder createNoFlutterSdkPanel(@NotNull EditorNotificationBuilder panel,
                                                                   @NotNull VirtualFile file,
                                                                   @NotNull Project project) {
    panel.withIcon(FlutterIcons.Flutter);
    panel.withText(FlutterBundle.messageValue("flutter.no.sdk.warning"));
    panel.withAction(LocalizeValue.localizeTODO("Dismiss"), e -> {
      file.putUserData(DISMISSED, Boolean.TRUE);
      EditorNotifications.getInstance(project).updateNotifications(file);
    });
    panel.withAction(LocalizeValue.localizeTODO("Open Flutter settings"), e -> FlutterUtils.openFlutterSettings(project));
    return panel;
  }
}
