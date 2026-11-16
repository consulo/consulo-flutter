/*
 * Copyright 2017 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.run.daemon;

import consulo.execution.configuration.CommandLineState;
import consulo.execution.ui.console.TextConsoleBuilder;
import consulo.ide.impl.idea.execution.filters.TextConsoleBuilderImpl;
import consulo.ide.impl.idea.execution.impl.ConsoleViewImpl;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.ui.console.ConsoleView;
import consulo.execution.ui.console.ConsoleViewContentType;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.execution.configuration.ExecutionSearchScopeProvider;
import consulo.content.scope.SearchScope;
import consulo.language.psi.scope.GlobalSearchScope;
import com.jetbrains.lang.dart.ide.runner.DartRelativePathsConsoleFilter;
import io.flutter.logging.PluginLogger;
import io.flutter.settings.FlutterSettings;
import io.flutter.utils.FlutterModuleUtils;
import io.flutter.utils.StdoutJsonParser;
import org.jetbrains.annotations.NotNull;

/**
 * A console view that filters out JSON messages sent in --machine mode.
 */
public class DaemonConsoleView extends ConsoleViewImpl {
  private static final @NotNull Logger LOG = PluginLogger.createLogger(DaemonConsoleView.class);

  /**
   * Sets up a launcher to use a DaemonConsoleView.
   */
  public static void install(@NotNull CommandLineState launcher, @NotNull ExecutionEnvironment env, @NotNull VirtualFile workDir) {
    // Create our own console builder.
    //
    // We need to filter input to this console without affecting other consoles, so we cannot use a consoleFilterInputProvider.
    final SearchScope searchScope = ExecutionSearchScopeProvider.createSearchScope(env.getProject(), env.getRunProfile()); // Consulo: no ExecutionSearchScopes
    final TextConsoleBuilder builder = new TextConsoleBuilderImpl(env.getProject(), searchScope) {
      @NotNull
      @Override
      protected ConsoleView createConsole() {
        return new DaemonConsoleView(env.getProject(), searchScope);
      }
    };

    // Set up basic console filters. (More may be added later.)
    // TODO(devoncarew): Do we need this filter? What about DartConsoleFilter (for package: uris)?
    builder.addFilter(new DartRelativePathsConsoleFilter(env.getProject(), workDir.getPath()));
    launcher.setConsoleBuilder(builder);
  }

  private final StdoutJsonParser stdoutParser = new StdoutJsonParser();
  private boolean hasPrintedText;

  public DaemonConsoleView(@NotNull final Project project, @NotNull final SearchScope searchScope) {
    super(project, searchScope, true, false);
  }

  @Override
  public void print(@NotNull String text, @NotNull ConsoleViewContentType contentType) {
    if (!FlutterModuleUtils.hasFlutterModule(getProject())) {
      return;
    }
    if (FlutterSettings.getInstance().isVerboseLogging()) {
      super.print(text, contentType);
      return;
    }

    if (contentType != ConsoleViewContentType.NORMAL_OUTPUT) {
      writeAvailableLines();
      super.print(text, contentType);
    }
    else {
      stdoutParser.appendOutput(text);
      writeAvailableLines();
    }
  }

  private void writeAvailableLines() {
    for (String line : stdoutParser.getAvailableLines()) {
      if (DaemonApi.parseAndValidateDaemonEvent(line.trim()) != null) {
        if (FlutterSettings.getInstance().isVerboseLogging()) {
          LOG.info(line.trim());
        }
      }
      else {
        // We're seeing a spurious newline before some launches; this removes any single newline that occurred
        // before we've printed text.
        if (!hasPrintedText && line.equals(("\n"))) {
          continue;
        }

        hasPrintedText = true;

        super.print(line, ConsoleViewContentType.NORMAL_OUTPUT);
      }
    }
  }
}
