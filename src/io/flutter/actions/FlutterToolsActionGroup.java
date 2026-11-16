/*
 * Copyright 2016 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.actions;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.flutter.localize.FlutterConsuloLocalize;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.IdeActions;
import io.flutter.icons.FlutterIcons;
import io.flutter.run.OpenDevToolsAction;

import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.Presentation;
import consulo.project.Project;
import io.flutter.utils.FlutterModuleUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ActionImpl(id = "FlutterToolsActionGroup", children = {
  @ActionRef(type = FlutterGettingStartedAction.class),
  @ActionRef(type = AnSeparator.class),
  @ActionRef(type = FlutterUpgradeAction.class),
  @ActionRef(type = FlutterDoctorAction.class),
  @ActionRef(type = AnSeparator.class),
  @ActionRef(type = FlutterPackagesAddAction.class),
  @ActionRef(type = FlutterPackagesGetAction.class),
  @ActionRef(type = FlutterPackagesUpgradeAction.class),
  @ActionRef(type = AnSeparator.class),
  @ActionRef(type = FlutterCleanAction.class),
  @ActionRef(type = AnSeparator.class),
  @ActionRef(type = OpenDevToolsAction.class),
  @ActionRef(type = AnSeparator.class),
  // Consulo: flutter.androidstudio.open is commented out upstream, flutter.appcode.open is dropped (AppCode is discontinued)
  @ActionRef(type = OpenInXcodeAction.class),
  @ActionRef(type = AnSeparator.class),
  @ActionRef(type = FlutterSubmitFeedback.class)
}, parents = @ActionParentRef(value = @ActionRef(id = IdeActions.TOOLS_MENU), anchor = ActionRefAnchor.LAST))
public class FlutterToolsActionGroup extends DefaultActionGroup implements AnActionWithSyncUpdate {
  public FlutterToolsActionGroup() {
    // Consulo: text, description, icon and popup of upstream plugin.xml <group>
    getTemplatePresentation().setText(FlutterConsuloLocalize.groupFluttertoolsactiongroupText());
    getTemplatePresentation().setDescription(FlutterConsuloLocalize.groupFluttertoolsactiongroupDescription());
    getTemplatePresentation().setIcon(FlutterIcons.Flutter);
    setPopup(true);
  }

  @Override
  public void update(@Nullable AnActionEvent e) {
    final Project project = e == null ? null : e.getData(Project.KEY);
    final Presentation presentation = e == null ? null : e.getPresentation();
    if (presentation != null) {
      final boolean visible = project != null && FlutterModuleUtils.declaresFlutter(project);
      presentation.setEnabled(visible);
      presentation.setVisible(visible);
    }
  }
}
