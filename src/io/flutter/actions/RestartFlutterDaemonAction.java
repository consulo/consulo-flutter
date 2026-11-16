/*
 * Copyright 2017 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.actions;

import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.project.Project;
import io.flutter.icons.FlutterIcons;
import io.flutter.run.daemon.DeviceService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import consulo.ui.image.Image;

public class RestartFlutterDaemonAction extends AnAction {

  /**
   * Create a `RestartFlutterDaemonAction` for presentation in the device selector.
   */
  public static RestartFlutterDaemonAction forDeviceSelector() {
    //noinspection DialogTitleCapitalization
    return new RestartFlutterDaemonAction("Restart Flutter Daemon", "Restart Flutter Daemon", FlutterIcons.Flutter);
  }

  /**
   * A default constructor, invoked by plugin.xml contributions.
   */
  RestartFlutterDaemonAction() {
    super();
  }

  /**
   * A constructor for dynamic invocation.
   */
  @SuppressWarnings("ActionPresentationInstantiatedInCtor")
  private RestartFlutterDaemonAction(@Nullable String text,
                                     @Nullable String description,
                                     @Nullable Image icon) {
    super(text, description, icon);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    final Project project = event.getData(Project.KEY);
    if (project == null) {
      return;
    }

    DeviceService.getInstance(project).restart();
  }
}
