/*
 * Copyright 2017 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.editor;

import consulo.annotation.component.ExtensionImpl;

import consulo.annotation.access.RequiredReadAction;
import consulo.fileEditor.EditorNotificationBuilder;
import consulo.fileEditor.EditorNotificationProvider;
import consulo.fileEditor.FileEditor;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import io.flutter.FlutterUtils;
import io.flutter.icons.FlutterIcons;
import io.flutter.pub.PubRoot;
import io.flutter.sdk.FlutterSdk;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

@ExtensionImpl
public final class FlutterPubspecNotificationProvider implements EditorNotificationProvider {
  @NotNull private final Project project;

  @Inject
  public FlutterPubspecNotificationProvider(@NotNull Project project) {
    this.project = project;
  }

  @NotNull
  @Override
  public String getId() {
    return "flutter.pubspec.notification";
  }

  // Consulo: EditorNotificationProvider fills an EditorNotificationBuilder instead of returning a panel factory
  @RequiredReadAction
  @Nullable
  @Override
  public EditorNotificationBuilder buildNotification(@NotNull VirtualFile file,
                                                     @NotNull FileEditor fileEditor,
                                                     @NotNull Supplier<EditorNotificationBuilder> builderFactory) {
    // We only show this notification inside of local pubspec files.
    if (!PubRoot.isPubspec(file) || !file.isInLocalFileSystem()) {
      return null;
    }

    // Check that this pubspec file declares flutter
    if (!FlutterUtils.declaresFlutter(file)) {
      return null;
    }

    final FlutterSdk flutterSdk = FlutterSdk.getFlutterSdk(project);
    if (flutterSdk == null) {
      return null;
    }

    return new FlutterPubspecActionsPanel(file, project, flutterSdk).fill(builderFactory.get());
  }

  static class FlutterPubspecActionsPanel {
    @NotNull final VirtualFile myFile;
    @NotNull final Project myProject;
    @NotNull final FlutterSdk myFlutterSdk;

    FlutterPubspecActionsPanel(@NotNull VirtualFile file, @NotNull Project project, @NotNull FlutterSdk flutterSdk) {
      myFile = file;
      myProject = project;
      myFlutterSdk = flutterSdk;
    }

    @NotNull
    EditorNotificationBuilder fill(@NotNull EditorNotificationBuilder builder) {
      builder.withIcon(FlutterIcons.Flutter);
      builder.withText(LocalizeValue.localizeTODO("Flutter commands"));

      // "flutter.pub.get"
      builder.withAction(LocalizeValue.localizeTODO("Pub get"), LocalizeValue.localizeTODO("Install referenced packages"),
                         e -> runPubGet(false));

      // "flutter.pub.upgrade"
      builder.withAction(LocalizeValue.localizeTODO("Pub upgrade"),
                         LocalizeValue.localizeTODO("Upgrade referenced packages to the latest versions"),
                         e -> runPubGet(true));

      // "flutter.pub.outdated"
      builder.withAction(LocalizeValue.localizeTODO("Pub outdated"),
                         LocalizeValue.localizeTODO("Analyze packages to determine which ones can be upgraded"),
                         e -> runPubOutdated());

      // Consulo: no separator between the links; the tooltip "Validate installed tools and their versions" is not supported
      // for action references.
      builder.withAction(LocalizeValue.localizeTODO("Flutter doctor"), "flutter.doctor");
      return builder;
    }

    private void runPubGet(boolean upgrade) {
      final PubRoot root = PubRoot.forDirectory(myFile.getParent());
      if (root != null) {
        if (!upgrade) {
          myFlutterSdk.startPubGet(root, myProject);
        }
        else {
          myFlutterSdk.startPubUpgrade(root, myProject);
        }
      }
    }

    private void runPubOutdated() {
      final PubRoot root = PubRoot.forDirectory(myFile.getParent());
      if (root != null) {
        myFlutterSdk.startPubOutdated(root, myProject);
      }
    }
  }
}
