/*
 * Copyright 2017 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.console;

import consulo.execution.ui.console.TextConsoleBuilder;
import consulo.execution.ui.console.TextConsoleBuilderFactory;
import consulo.process.ProcessHandler;
import consulo.process.event.ProcessEvent;
import consulo.execution.ui.console.ConsoleView;
import consulo.execution.ui.console.ConsoleViewContentType;
import consulo.module.Module;
import consulo.project.Project;
import consulo.ui.ex.awt.SimpleToolWindowPanel;
import consulo.disposer.Disposer;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.content.ContentManager;
import consulo.project.ui.view.MessageView;
import io.flutter.utils.ProcessAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A console showing output from a Flutter command.
 */
class FlutterConsole {
  @NotNull final ConsoleView view;
  @NotNull final Content content;
  @NotNull final Project project;
  @Nullable final Module module;

  @Nullable
  private Runnable cancelProcessSubscription;

  private FlutterConsole(@NotNull ConsoleView view, @NotNull Content content, @NotNull Project project, @Nullable Module module) {
    this.view = view;
    this.content = content;
    this.project = project;
    this.module = module;
  }

  @NotNull
  static FlutterConsole create(@NotNull Project project, @Nullable Module module) {
    var builderFactory = TextConsoleBuilderFactory.getInstance();
    assert builderFactory != null;
    final TextConsoleBuilder builder = builderFactory.createBuilder(project);
    builder.setViewer(true);
    if (module != null) {
      builder.addFilter(new FlutterConsoleFilter(module));
    }
    final ConsoleView view = builder.getConsole();

    final SimpleToolWindowPanel panel = new SimpleToolWindowPanel(false, true);
    panel.setContent(view.getComponent());

    final String title = module != null ? "[" + module.getName() + "] Flutter" : "Flutter";
    ContentFactory contentFactory = ContentFactory.getInstance();
    assert contentFactory != null;
    final Content content = contentFactory.createContent(panel.getComponent(), title, true);
    Disposer.register(content, view);

    return new FlutterConsole(view, content, project, module);
  }

  /**
   * Starts displaying the output of a different process.
   */
  void watchProcess(@NotNull ProcessHandler process) {
    if (cancelProcessSubscription != null) {
      cancelProcessSubscription.run();
      cancelProcessSubscription = null;
    }

    view.clear();
    view.attachToProcess(process);

    // Print exit code.
    final ProcessAdapter listener = new ProcessAdapter() {
      @Override
      public void processTerminated(final @NotNull ProcessEvent event) {
        view.print(
          "Process finished with exit code " + event.getExitCode(),
          ConsoleViewContentType.SYSTEM_OUTPUT);
      }
    };
    process.addProcessListener(listener);
    cancelProcessSubscription = () -> process.removeProcessListener(listener);
  }

  /**
   * Moves this console to the end of the tool window's tab list, selects it, and shows the tool window.
   */
  void bringToFront() {
    // Move the tab to be last and select it.
    final MessageView messageView = MessageView.getInstance(project);
    final ContentManager contentManager = messageView.getContentManager();
    contentManager.addContent(content);
    contentManager.setSelectedContent(content);

    // Show the panel.
    final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.MESSAGES_WINDOW);
    if (toolWindow != null) {
      toolWindow.activate(null, true);
    }
  }
}
