/*
 * Copyright 2016 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.actions;

import consulo.ui.ex.action.AnActionEvent;
import consulo.dataContext.DataContext;
import consulo.document.FileDocumentManager;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.project.Project;
import io.flutter.FlutterBundle;
import io.flutter.FlutterMessages;
import io.flutter.FlutterUtils;
import io.flutter.pub.PubRoot;
import io.flutter.pub.PubRoots;
import io.flutter.sdk.FlutterSdk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Base class for Flutter commands.
 */
public abstract class FlutterSdkAction extends DumbAwareAction {

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    final Project project = event.getData(Project.KEY);

    final FlutterSdk sdk = project != null ? FlutterSdk.getFlutterSdk(project) : null;
    if (sdk == null) {
      showMissingSdkDialog(project);
      return;
    }

    FileDocumentManager.getInstance().saveAllDocuments();
    PubRoot root = PubRoot.forEventWithRefresh(event);
    @NotNull DataContext context = event.getDataContext();
    if (root != null) {
      startCommand(project, sdk, root, context);
    }
    else {
      List<PubRoot> roots = PubRoots.forProject(project);
      for (PubRoot sub : roots) {
        startCommand(project, sdk, sub, context);
      }
    }
  }

  public abstract void startCommand(@NotNull Project project,
                                    @NotNull FlutterSdk sdk,
                                    @Nullable PubRoot root,
                                    @NotNull DataContext context);

  public static void showMissingSdkDialog(@Nullable Project project) {
    final int response = FlutterMessages.showDialog(project, FlutterBundle.message("flutter.sdk.notAvailable.message"),
                                                    FlutterBundle.message("flutter.sdk.notAvailable.title"),
                                                    new String[]{"Yes, configure", "No, thanks"}, -1);
    if (response == 0) {
      FlutterUtils.openFlutterSettings(project);
    }
  }
}
