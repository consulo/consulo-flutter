/*
 * Copyright 2024 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.view;

import consulo.annotation.component.ExtensionImpl;

import consulo.component.PropertiesComponent;
import consulo.project.ProjectPropertiesComponent;
import consulo.application.dumb.DumbAware;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.project.ui.wm.ToolWindowFactory;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import io.flutter.icons.FlutterIcons;
import io.flutter.utils.OpenApiUtils;
import org.jetbrains.annotations.NotNull;

@ExtensionImpl
public class InspectorViewFactory implements ToolWindowFactory, DumbAware {
  private static final String TOOL_WINDOW_VISIBLE_PROPERTY = "flutter.view.tool.window.visible";

  public static void init(@NotNull Project project) {
    project.getMessageBus().connect().subscribe(
      FlutterViewMessages.FLUTTER_DEBUG_TOPIC, (FlutterViewMessages.FlutterDebugNotifier)(event) -> initInspectorView(project, event)
    );

    final ToolWindow window = ToolWindowManager.getInstance(project).getToolWindow(InspectorView.TOOL_WINDOW_ID);
    if (window != null) {
      window.setAvailable(true);

      if (ProjectPropertiesComponent.getInstance(project).getBoolean(TOOL_WINDOW_VISIBLE_PROPERTY, false)) {
        window.activate(null, false);
      }
    }
  }

  // Consulo: the tool window id, anchor, icon and display name of upstream plugin.xml are provided by the factory
  @NotNull
  @Override
  public String getId() {
    return InspectorView.TOOL_WINDOW_ID;
  }

  @NotNull
  @Override
  public ToolWindowAnchor getAnchor() {
    return ToolWindowAnchor.RIGHT;
  }

  @NotNull
  @Override
  public Image getIcon() {
    return FlutterIcons.DevToolsInspector;
  }

  @NotNull
  @Override
  public LocalizeValue getDisplayName() {
    return LocalizeValue.localizeTODO("Flutter Inspector");
  }

  @Override
  public boolean shouldBeAvailable(@NotNull Project project) {
    return false;
  }

  private static void initInspectorView(@NotNull Project project, FlutterViewMessages.FlutterDebugEvent event) {
    OpenApiUtils.safeInvokeLater(() -> {
      final InspectorView inspectorView = project.getInstance(InspectorView.class);
      inspectorView.debugActive(event);
    });
  }

  @Override
  public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
    //noinspection CodeBlock2Expr
    DumbService.getInstance(project).runWhenSmart(() -> {
      project.getInstance(InspectorView.class).initToolWindow(toolWindow);
    });
  }
}
