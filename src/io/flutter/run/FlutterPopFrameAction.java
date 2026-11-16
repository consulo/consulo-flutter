/*
 * Copyright 2017 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.run;

import consulo.execution.debug.icon.ExecutionDebugIconGroup;
import consulo.ui.ex.action.*;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.XDebuggerManager;
import consulo.execution.debug.frame.XStackFrame;
import io.flutter.FlutterBundle;
import io.flutter.vmService.frame.DartVmServiceStackFrame;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FlutterPopFrameAction extends AnAction implements DumbAware, AnActionWithSyncUpdate {
  public FlutterPopFrameAction() {
    final Presentation presentation = getTemplatePresentation();
    presentation.setText(FlutterBundle.messageValue("flutter.pop.frame.action.text"));
    presentation.setDescription(FlutterBundle.messageValue("flutter.pop.frame.action.description"));
    presentation.setIcon(ExecutionDebugIconGroup.actionInlinedropframe()); // Consulo: no Actions.PopFrame icon
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final DartVmServiceStackFrame frame = getStackFrame(e);
    if (frame != null) {
      frame.dropFrame();
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    final DartVmServiceStackFrame frame = getStackFrame(e);
    final boolean enabled = frame != null && frame.canDrop();

    // Consulo: no ActionPlaces.isMainMenuOrActionSearch
    final boolean isMainMenuOrSearch = ActionPlaces.MAIN_MENU.equals(e.getPlace()) || ActionPlaces.ACTION_SEARCH.equals(e.getPlace());
    if (isMainMenuOrSearch || ActionPlaces.DEBUGGER_TOOLBAR.equals(e.getPlace())) {
      e.getPresentation().setEnabled(enabled);
    }
    else {
      e.getPresentation().setVisible(enabled);
    }
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }

  @Nullable
  private static DartVmServiceStackFrame getStackFrame(@NotNull final AnActionEvent e) {
    final Project project = e.getData(Project.KEY);
    if (project == null) return null;

    XDebugSession session = e.getData(XDebugSession.DATA_KEY);

    if (session == null) {
      session = XDebuggerManager.getInstance(project).getCurrentSession();
    }

    if (session != null) {
      final XStackFrame frame = session.getCurrentStackFrame();
      if (frame instanceof DartVmServiceStackFrame) {
        return ((DartVmServiceStackFrame)frame);
      }
    }

    return null;
  }
}
