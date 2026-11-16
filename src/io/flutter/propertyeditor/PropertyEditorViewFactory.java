/*
 * Copyright 2025 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.propertyeditor;

import consulo.project.Project;
import consulo.ui.image.Image;
import io.flutter.icons.FlutterIcons;
import consulo.disposer.Disposer;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.ex.toolWindow.ToolWindowType;
import consulo.project.ui.wm.ToolWindowManagerListener;
import consulo.component.messagebus.MessageBusConnection;
import io.flutter.dart.DartPlugin;
import io.flutter.dart.DartPluginVersion;
import io.flutter.devtools.AbstractDevToolsViewFactory;
import io.flutter.devtools.DevToolsIdeFeature;
import io.flutter.devtools.DevToolsUrl;
import io.flutter.run.daemon.DevToolsInstance;
import io.flutter.sdk.FlutterSdkVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public class PropertyEditorViewFactory extends AbstractDevToolsViewFactory {
  @NotNull public final static String TOOL_WINDOW_ID = "Flutter Property Editor";

  @NotNull public final static String DEVTOOLS_PAGE_ID = "propertyEditor";
  @Nullable private static Boolean previousDockedUnpinned = null;

  @Override
  public boolean versionSupportsThisTool(@NotNull final FlutterSdkVersion flutterSdkVersion) {
    return flutterSdkVersion.canUsePropertyEditor();
  }

  @Override
  @NotNull
  public String getToolWindowId() {
    return TOOL_WINDOW_ID;
  }

  // Consulo: the tool window icon of upstream plugin.xml
  @Override
  @NotNull
  public Image getIcon() {
    return FlutterIcons.PropertyEditor;
  }

  @Override
  @NotNull
  public String getToolWindowTitle() {
    return "Property Editor";
  }

  @Override
  @NotNull
  public DevToolsUrl getDevToolsUrl(@NotNull final Project project,
                                    @NotNull final FlutterSdkVersion flutterSdkVersion,
                                    @NotNull final DevToolsInstance instance) {
    return new DevToolsUrl.Builder()
      .setDevToolsHost(instance.host())
      .setDevToolsPort(instance.port())
      .setPage(DEVTOOLS_PAGE_ID)
      .setEmbed(true)
      .setFlutterSdkVersion(flutterSdkVersion)
      .setIdeFeature(DevToolsIdeFeature.TOOL_WINDOW)
      .build();
  }

  @Override
  public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
    final DartPluginVersion dartPluginVersion = DartPlugin.getDartPluginVersion();
    if (!dartPluginVersion.supportsPropertyEditor()) {
      super.viewUtils.presentLabels(toolWindow, List.of("The Flutter Property Editor requires a",
                                                        "newer version of the Dart plugin."));
      return;
    }

    checkDockedUnpinnedAndCreateContent(project, toolWindow, true);

    final PropertyEditorViewFactory self = this;
    // Consulo: the content manager is the tool window disposable
    MessageBusConnection connection = project.getMessageBus().connect(toolWindow.getContentManager());
    connection.subscribe(ToolWindowManagerListener.class, new ToolWindowManagerListener() {
      @Override
      public void toolWindowShown(@NotNull ToolWindow activatedToolWindow) {
        if (activatedToolWindow.getId().equals(getToolWindowId())) {
          checkDockedUnpinnedAndCreateContent(project, toolWindow, false);
        }
      }

      // Consulo: stateChanged has no event type, so any state change is checked (upstream: SetToolWindowAutoHide and
      // SetToolWindowType only); the content is only reloaded when the docked unpinned state changes.
      @Override
      public void stateChanged(@NotNull ToolWindowManager toolWindowManager) {
        checkDockedUnpinnedAndCreateContent(project, toolWindow, false);
      }
    });
  }

  private void checkDockedUnpinnedAndCreateContent(@NotNull Project project, @NotNull ToolWindow toolWindow, boolean forceLoad) {
    final Boolean isDockedUnpinned = toolWindow.getType().equals(ToolWindowType.DOCKED) && toolWindow.isAutoHide();

    // If this is the first time we are loading the content, force a load even if docked unpinned state matches.
    // Docked unpinned state is only null the first time application is opened (I think).
    if (!isDockedUnpinned.equals(previousDockedUnpinned) || forceLoad) {
      previousDockedUnpinned = isDockedUnpinned;
      super.createToolWindowContent(project, toolWindow, isDockedUnpinned
                                                         ? "This tool window is in \"Docked Unpinned\" mode, which means it will disappear "
                                                           + "during normal use of the property editor. Select Options (three dots) > View "
                                                           + "Mode > Docked Pinned instead."
                                                         : null);
    }
  }
}
