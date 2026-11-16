/*
 * Copyright 2019 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.utils;

import consulo.process.ExecutionException;
import consulo.process.ProcessHandler;
import consulo.process.ProcessHandlerBuilder;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.event.ProcessEvent;
import consulo.process.event.ProcessListener;
import consulo.process.io.BaseOutputReader;
import consulo.util.dataholder.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * An {@link ColoredProcessHandler} that uses {@code BaseOutputReader.Options.forMostlySilentProcess}
 * in order to reduce cpu usage of the process it runs.
 *
 * <p>
 * This works by defaulting to a non-blocking process polling mode instead of a blocking mode.
 * The default can be overriden by setting the following registry flags:
 * <ul>
 *   <li> "output.reader.blocking.mode.for.mostly.silent.processes" = false
 *   <li> "output.reader.blocking.mode" = true
 * </ul>
 *
 * <p>
 * Note that long-running processes that don't use these options may log a warning in message
 * in the IntelliJ log.  See {@link BaseOSProcessHandler}'s {@code SimpleOutputReader.beforeSleeping}
 * for more information.
 */
public class MostlySilentColoredProcessHandler {
  // Consulo: the colored OS process handler is internal and can't be subclassed, so this builds an equivalent handler:
  // colored output, `forMostlySilentProcess` reader options, and SIGINT to the process tree on Unix (soft kill)
  // as upstream's doDestroyProcess override. onTextAvailable gets the decoded text, as upstream's coloredTextAvailable.
  private MostlySilentColoredProcessHandler() {
  }

  public static @NotNull ProcessHandler create(@NotNull GeneralCommandLine commandLine) throws ExecutionException {
    return create(commandLine, null);
  }

  public static @NotNull ProcessHandler create(@NotNull GeneralCommandLine commandLine, @Nullable Consumer<String> onTextAvailable)
    throws ExecutionException {
    ProcessHandler handler = ProcessHandlerBuilder.create(commandLine)
      .colored()
      .killable()
      .shouldKillProcessSoftly(true)
      .readerOptions(BaseOutputReader.Options.forMostlySilentProcess())
      .build();
    if (onTextAvailable != null) {
      handler.addProcessListener(new ProcessListener() {
        @Override
        public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
          onTextAvailable.accept(event.getText());
        }
      });
    }
    return handler;
  }
}
