/*
 * Copyright 2017 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.actions;

import consulo.application.dumb.DumbAware;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.IdeActions;
import io.flutter.FlutterBundle;
import io.flutter.FlutterUtils;
import org.jetbrains.annotations.NotNull;

/**
 * This classes was re-implemented with Kotlin from FlutterNewProjectAction.java to resolve the New Flutter Project action from hanging,
 * https://github.com/flutter/flutter-intellij/issues/8310.
 *
 * See https://github.com/JetBrains/intellij-community/blob/master/java/idea-ui/src/com/intellij/ide/impl/NewProjectUtil.kt
 */
// Consulo: converted back to Java; the project wizard is opened through the platform "NewProject" action
// (Consulo has no public NewProjectWizard / createNewProjectAsync API).
public class FlutterNewProjectAction extends AnAction implements DumbAware, AnActionWithSyncUpdate {
  @Override
  public void update(@NotNull AnActionEvent e) {
    // Consulo: no NewWelcomeScreen.isNewWelcomeScreen(e), check the welcome screen place
    if (ActionPlaces.WELCOME_SCREEN.equals(e.getPlace())) {
      e.getPresentation().setText(FlutterBundle.messageValue("welcome.new.project.compact"));
    }
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    if (FlutterUtils.isAndroidStudio()) {
      System.setProperty("studio.projectview", "true");
    }

    final AnAction newProjectAction = ActionManager.getInstance().getAction(IdeActions.ACTION_NEW_PROJECT);
    if (newProjectAction != null) {
      newProjectAction.actionPerformed(e);
    }
  }
}
