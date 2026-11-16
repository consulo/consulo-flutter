/*
 * Copyright 2018 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.actions;

import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.execution.ProgramRunnerUtil;
import consulo.execution.RunManager;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.event.ExecutionListener;
import consulo.execution.executor.Executor;
import consulo.execution.configuration.RunConfiguration;
import consulo.process.ProcessHandler;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.runner.ExecutionEnvironmentBuilder;
import consulo.ui.ex.action.AnActionEvent;
import consulo.dataContext.DataContext;
import consulo.ui.ModalityState;
import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.Messages;
import consulo.util.dataholder.Key;
import consulo.project.ui.wm.ToolWindowId;
import consulo.util.lang.ThreeState;
import consulo.component.messagebus.MessageBusConnection;
import io.flutter.FlutterConstants;
import io.flutter.pub.PubRoot;
import io.flutter.run.FlutterLaunchMode;
import io.flutter.run.SdkAttachConfig;
import io.flutter.run.SdkRunConfig;
import io.flutter.sdk.FlutterSdk;
import io.flutter.sdk.FlutterSdkUtil;
import io.flutter.utils.OpenApiUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.function.Consumer;

public class AttachDebuggerAction extends FlutterSdkAction implements AnActionWithSyncUpdate {

  // Is 'flutter attach' running for a project?
  // The three states are true, false, and non-existent (null).
  //   true = run automatically via the debug listener in FlutterStudioStartupActivity
  //   false = run from button press here
  //   null = no attach process is running
  // The button is still required because there may be multiple Flutter run configs.
  public static final Key<ThreeState> ATTACH_IS_ACTIVE = new Key<>("attach-is-active");

  @Override
  public void startCommand(@NotNull Project project, @NotNull FlutterSdk sdk, @Nullable PubRoot root, @NotNull DataContext context) {
    // NOTE: When making changes here, consider making similar changes to RunFlutterAction.
    RunConfiguration configuration = findRunConfig(project);
    if (configuration == null) {
      final RunnerAndConfigurationSettings settings = RunManager.getInstance(project).getSelectedConfiguration();
      if (settings == null) {
        showSelectConfigDialog();
        return;
      }
      configuration = settings.getConfiguration();
      if (!(configuration instanceof SdkRunConfig)) {
        if (project.isDefault() || !FlutterSdkUtil.hasFlutterModules(project)) {
          return;
        }
        showSelectConfigDialog();
        return;
      }
    }

    final SdkAttachConfig sdkRunConfig = new SdkAttachConfig((SdkRunConfig)configuration);
    sdkRunConfig.pubRoot = root;
    final Executor executor = RunFlutterAction.getExecutor(ToolWindowId.DEBUG);
    if (executor == null) {
      return;
    }

    final ExecutionEnvironmentBuilder builder = ExecutionEnvironmentBuilder.create(executor, sdkRunConfig);

    final ExecutionEnvironment env = builder.activeTarget().dataContext(context).build();
    FlutterLaunchMode.addToEnvironment(env, FlutterLaunchMode.DEBUG);

    if (project.getUserData(ATTACH_IS_ACTIVE) == null) {
      project.putUserData(ATTACH_IS_ACTIVE, ThreeState.fromBoolean(false));
      onAttachTermination(project, (p) -> p.putUserData(ATTACH_IS_ACTIVE, null));
    }
    ProgramRunnerUtil.executeConfiguration(env, false, true);
  }


  @Override
  public void update(@NotNull AnActionEvent e) {
    final Project project = e.getData(Project.KEY);
    if (project == null || project.isDefault()) {
      // Consulo: AnAction has no update() to call, the default did nothing
      return;
    }
    if (!FlutterSdkUtil.hasFlutterModules(project)) {
      // Hide this button in Android projects.
      e.getPresentation().setVisible(false);
      return;
    }
    RunConfiguration configuration = findRunConfig(project);
    boolean enabled;
    if (configuration == null) {
      final RunnerAndConfigurationSettings settings = RunManager.getInstance(project).getSelectedConfiguration();
      if (settings == null) {
        enabled = false;
      }
      else {
        configuration = settings.getConfiguration();
        enabled = configuration instanceof SdkRunConfig;
      }
    }
    else {
      enabled = true;
    }
    if (enabled && (project.getUserData(ATTACH_IS_ACTIVE) != null)) {
      enabled = false;
    }
    e.getPresentation().setVisible(true);
    e.getPresentation().setEnabled(enabled);
  }

  @Nullable
  public static RunConfiguration findRunConfig(@NotNull Project project) {
    // Look for a Flutter run config. If exactly one is found then return it otherwise return null.
    final RunManager mgr = RunManager.getInstance(project);
    final List<RunConfiguration> configs = mgr.getAllConfigurationsList();
    int count = 0;
    RunConfiguration sdkConfig = null;
    for (RunConfiguration config : configs) {
      if (config instanceof SdkRunConfig) {
        count += 1;
        sdkConfig = config;
      }
    }
    return count == 1 ? sdkConfig : null;
  }

  private static void onAttachTermination(@NotNull Project project, @NotNull Consumer<@NotNull Project> runner) {
    final MessageBusConnection connection = project.getMessageBus().connect();

    // Need an ExecutionListener to clean up project-scoped state when the Stop button is clicked.
    connection.subscribe(ExecutionListener.class, new ExecutionListener() {
      Object handler;

      @Override
      public void processStarted(@NotNull String executorId, @NotNull ExecutionEnvironment env, @NotNull ProcessHandler handler) {
        if (env.getRunProfile() instanceof SdkAttachConfig) {
          this.handler = handler;
        }
      }

      @Override
      public void processTerminated(@NotNull String executorId,
                                    @NotNull ExecutionEnvironment env,
                                    @NotNull ProcessHandler handler,
                                    int exitCode) {
        if (this.handler == handler) {
          runner.accept(project);
          connection.disconnect();
        }
      }
    });
  }

  private static void showSelectConfigDialog() {
    OpenApiUtils.safeInvokeLater(() -> new SelectConfigDialog().show(), ModalityState.nonModal());
  }

  private static class SelectConfigDialog extends DialogWrapper {
    private JPanel myPanel;
    private JTextPane myTextPane;

    SelectConfigDialog() {
      super(null, false, false);
      setTitle("Run Configuration");
      myPanel = new JPanel();
      myTextPane = new JTextPane();
      Messages.installHyperlinkSupport(myTextPane);
      final String selectConfig = "<html><body>" +
                                  "<p>The run configuration for the Flutter module must be selected." +
                                  "<p>Please change the run configuration to the one created when the<br>" +
                                  "module was created. See <a href=\"" +
                                  FlutterConstants.URL_RUN_AND_DEBUG +
                                  "\">the Flutter documentation</a> for more information.</body></html>";
      //noinspection DataFlowIssue
      myTextPane.setText(selectConfig);
      //noinspection DataFlowIssue
      myPanel.add(myTextPane);
      init();
      getButton(getCancelAction()).setVisible(false);
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
      return myPanel;
    }
  }
}
