/*
 * Copyright 2017 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.view;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import jakarta.inject.Singleton;
import jakarta.inject.Inject;

import com.google.common.annotations.VisibleForTesting;
import consulo.webBrowser.BrowserLauncher;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.project.Project;
import consulo.disposer.Disposer;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.awt.UIUtil;
import io.flutter.FlutterBundle;
import io.flutter.FlutterUtils;
import io.flutter.actions.RefreshToolWindowAction;
import io.flutter.devtools.DevToolsIdeFeature;
import io.flutter.devtools.DevToolsUrl;
import io.flutter.devtools.DevToolsUtils;
import io.flutter.logging.PluginLogger;
import io.flutter.run.FlutterDevice;
import io.flutter.run.daemon.DevToolsInstance;
import io.flutter.run.daemon.DevToolsService;
import io.flutter.run.daemon.FlutterApp;
import io.flutter.sdk.FlutterSdk;
import io.flutter.sdk.FlutterSdkVersion;
import io.flutter.settings.FlutterSettings;
import io.flutter.toolwindow.InspectorViewToolWindowManagerListener;
import io.flutter.utils.AsyncUtils;
import io.flutter.utils.LabelInput;
import io.flutter.utils.OpenApiUtils;
import org.dartlang.vm.service.VmService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import consulo.ui.image.Image;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

// Consulo: upstream @State is dropped, this service has no persistent state.
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
public class InspectorView implements Disposable {
  private static final @NotNull Logger LOG = PluginLogger.createLogger(InspectorView.class);

  public static final @NotNull String TOOL_WINDOW_ID = "Flutter Inspector";

  // Consulo: the JxBrowser labels are unused
  protected static final String INSTALLATION_IN_PROGRESS_LABEL = "Installing JxBrowser and DevTools...";
  protected static final String INSTALLATION_TIMED_OUT_LABEL =
    "Waiting for JxBrowser installation timed out. Restart your IDE to try again.";
  protected static final String INSTALLATION_WAIT_FAILED = "The JxBrowser installation failed unexpectedly. Restart your IDE to try again.";
  protected static final String DEVTOOLS_FAILED_LABEL = "Setting up DevTools failed.";
  protected static final int INSTALLATION_WAIT_LIMIT_SECONDS = 2000;

  @VisibleForTesting
  @NotNull
  public final ViewUtils viewUtils;

  @NotNull
  private final Project myProject;

  private @Nullable Content emptyContent;

  private InspectorViewToolWindowManagerListener toolWindowListener;
  private int devToolsInstallCount = 0;
  // Consulo: JxBrowser is dropped (JxBrowserManager, JxBrowserUtils and the installation handling).
  @Inject
  public InspectorView(@NotNull Project project) {
    this(project, new ViewUtils());
  }

  @VisibleForTesting
  protected InspectorView(@NotNull Project project,
                          ViewUtils viewUtils) {
    myProject = project;
    this.viewUtils = viewUtils != null ? viewUtils : new ViewUtils();
  }

  @Override
  public void dispose() {
    Disposer.dispose(this);
  }

  void initToolWindow(@NotNull ToolWindow window) {
    if (window.isDisposed()) return;

    updateForEmptyContent(window);
  }

  private void addBrowserInspectorViewContent(@NotNull FlutterApp app,
                                              @NotNull ToolWindow toolWindow,
                                              boolean isEmbedded,
                                              DevToolsIdeFeature ideFeature,
                                              @NotNull DevToolsInstance devToolsInstance) {
    assert (SwingUtilities.isEventDispatchThread());

    final ContentManager contentManager = toolWindow.getContentManager();

    final FlutterDevice device = app.device();
    final List<FlutterDevice> existingDevices = new ArrayList<>();
    final String tabName = device.getUniqueName(existingDevices);
    final Image tabIcon = device.getIcon();

    if (emptyContent != null) {
      contentManager.removeContent(emptyContent, true);
      emptyContent = null;
    }

    final String browserUrl = app.getConnector().getBrowserUrl();
    FlutterSdk flutterSdk = FlutterSdk.getFlutterSdk(app.getProject());
    FlutterSdkVersion flutterSdkVersion = flutterSdk == null ? null : flutterSdk.getVersion();

    // Register for devtools events (required for inspector->editor source linking)
    // See: https://github.com/flutter/flutter-intellij/issues/8041
    VmService vmService = app.getVmService();
    if (vmService != null) {
      DevToolsUtils.registerDevToolsVmServiceListener(app);
    }

    if (isEmbedded) {
      final DevToolsUrl devToolsUrl = new DevToolsUrl.Builder()
        .setDevToolsHost(devToolsInstance.host())
        .setDevToolsPort(devToolsInstance.port())
        .setVmServiceUri(browserUrl)
        .setPage("inspector")
        .setEmbed(true)
        .setFlutterSdkVersion(flutterSdkVersion)
        .setIdeFeature(ideFeature)
        .build();

      //noinspection CodeBlock2Expr

      Runnable task = () -> {
        embeddedBrowserOptional().ifPresent(
          embeddedBrowser -> OpenApiUtils.safeInvokeLater(() -> {
            embeddedBrowser.openPanel(toolWindow, tabName, tabIcon, new DevToolsUrlProvider(devToolsUrl), (String error) -> {
              // If the embedded browser doesn't work, offer a link to open in the regular browser.
              final List<LabelInput> inputs = Arrays.asList(
                new LabelInput("The embedded browser failed to load. Error: " + error),
                openDevToolsLabel(app, toolWindow, ideFeature)
              );
              viewUtils.presentClickableLabel(toolWindow, inputs);
            });
          }));
      };
      final ProgressManager progressManager = ProgressManager.getInstance();
      progressManager.runProcess(task, new EmptyProgressIndicator());

      toolWindow.setTitleActions(new RefreshToolWindowAction(TOOL_WINDOW_ID));
    }
    else {
      BrowserLauncher.getInstance().browse(
        new DevToolsUrl.Builder()
          .setDevToolsHost(devToolsInstance.host())
          .setDevToolsPort(devToolsInstance.port())
          .setVmServiceUri(browserUrl)
          .setPage("inspector")
          .setFlutterSdkVersion(flutterSdkVersion)
          .setIdeFeature(ideFeature)
          .build()
          .getUrlString(),
        null
      );
      viewUtils.presentLabel(toolWindow, "DevTools inspector has been opened in the browser.");
    }
  }

  private @NotNull Optional<EmbeddedBrowser> embeddedBrowserOptional() {
    if (myProject.isDisposed()) {
      return Optional.empty();
    }
    return Optional.ofNullable(FlutterUtils.embeddedBrowser(myProject));
  }

  /**
   * Called when a debug connection starts.
   */
  public void debugActive(@NotNull FlutterViewMessages.FlutterDebugEvent event) {
    final FlutterApp app = event.app;
    if (app.getFlutterDebugProcess() == null) {
      return;
    }
    OpenApiUtils.safeInvokeLater(() -> debugActiveHelper(app));
  }

  private void presentDevTools(FlutterApp app, ToolWindow toolWindow, boolean isEmbedded, DevToolsIdeFeature ideFeature) {
    verifyEventDispatchThread();

    devToolsInstallCount += 1;
    viewUtils.presentLabel(toolWindow, getInstallingDevtoolsLabel());

    openInspectorWithDevTools(app, toolWindow, isEmbedded, ideFeature);

    setUpToolWindowListener(app, toolWindow, isEmbedded, ideFeature);
  }

  @VisibleForTesting
  protected void verifyEventDispatchThread() {
    assert (SwingUtilities.isEventDispatchThread());
  }

  @VisibleForTesting
  protected void setUpToolWindowListener(FlutterApp app, ToolWindow toolWindow, boolean isEmbedded, DevToolsIdeFeature ideFeature) {
    if (this.toolWindowListener == null) {
      this.toolWindowListener = new InspectorViewToolWindowManagerListener(myProject, toolWindow);
    }
    this.toolWindowListener.updateOnWindowOpen(() -> {
      devToolsInstallCount += 1;
      viewUtils.presentLabel(toolWindow, getInstallingDevtoolsLabel());
      openInspectorWithDevTools(app, toolWindow, isEmbedded, ideFeature, true);
    });
  }

  private String getInstallingDevtoolsLabel() {
    return "<html><body style=\"text-align: center;\">" +
           FlutterBundle.message("flutter.devtools.installing", devToolsInstallCount) + "</body></html>";
  }

  @VisibleForTesting
  protected void openInspectorWithDevTools(FlutterApp app, ToolWindow toolWindow, boolean isEmbedded, DevToolsIdeFeature ideFeature) {
    openInspectorWithDevTools(app, toolWindow, isEmbedded, ideFeature, false);
  }

  private void openInspectorWithDevTools(FlutterApp app,
                                         ToolWindow toolWindow,
                                         boolean isEmbedded,
                                         DevToolsIdeFeature ideFeature,
                                         boolean forceDevToolsRestart) {
    if (toolWindow == null) {
      LOG.error("Unable to open Inspector with DevTools: toolwindow is null");
      return;
    }
    AsyncUtils.whenCompleteUiThread(
      forceDevToolsRestart
      ? DevToolsService.getInstance(myProject).getDevToolsInstanceWithForcedRestart()
      : DevToolsService.getInstance(myProject).getDevToolsInstance(),
      (instance, error) -> {
        // Skip displaying if the project has been closed.
        if (!myProject.isOpen()) {
          return;
        }

        // TODO(helinx): Restart DevTools server if there's an error.
        if (error != null) {
          FlutterUtils.error(LOG, "Error while starting DevTools server", error, true);
          viewUtils.presentLabel(toolWindow, DEVTOOLS_FAILED_LABEL);
          return;
        }

        if (instance == null || app == null) {
          viewUtils.presentLabel(toolWindow, DEVTOOLS_FAILED_LABEL);
          return;
        }

        addBrowserInspectorViewContent(app, toolWindow, isEmbedded, ideFeature, instance);
      }
    );
  }

  private LabelInput openDevToolsLabel(FlutterApp app, ToolWindow toolWindow, DevToolsIdeFeature ideFeature) {
    return new LabelInput("Open DevTools in the browser?", (linkLabel, data) -> {
      presentDevTools(app, toolWindow, false, ideFeature);
    });
  }

  protected void presentOpenDevToolsOptionWithMessage(FlutterApp app,
                                                      ToolWindow toolWindow,
                                                      String message,
                                                      DevToolsIdeFeature ideFeature) {
    final List<LabelInput> inputs = new ArrayList<>();
    inputs.add(new LabelInput(message));
    inputs.add(openDevToolsLabel(app, toolWindow, ideFeature));
    viewUtils.presentClickableLabel(toolWindow, inputs);
  }

  private void debugActiveHelper(@NotNull FlutterApp app) {
    final ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(myProject);
    if (toolWindowManager == null) {
      return;
    }

    final ToolWindow toolWindow = toolWindowManager.getToolWindow(InspectorView.TOOL_WINDOW_ID);
    if (toolWindow == null) {
      return;
    }

    AtomicReference<DevToolsIdeFeature> ideFeature = new AtomicReference<>(null);
    if (toolWindow.isAvailable()) {
      ideFeature.set(updateToolWindowVisibility(toolWindow));
    }
    else {
      toolWindow.setAvailable(true, () -> {
        ideFeature.set(updateToolWindowVisibility(toolWindow));
      });
    }

    if (emptyContent != null) {
      final ContentManager contentManager = toolWindow.getContentManager();
      contentManager.removeContent(emptyContent, true);
      emptyContent = null;
    }

    if (toolWindow.isVisible()) {
      displayEmbeddedBrowser(app, toolWindow, ideFeature.get());
    }
    else {
      if (toolWindowListener == null) {
        toolWindowListener = new InspectorViewToolWindowManagerListener(myProject, toolWindow);
      }
      // If the window isn't visible yet, only executed embedded browser steps when it becomes visible.
      toolWindowListener.updateOnWindowFirstVisible(() -> {
        displayEmbeddedBrowser(app, toolWindow, DevToolsIdeFeature.TOOL_WINDOW);
      });
    }
  }

  private void displayEmbeddedBrowser(FlutterApp app, ToolWindow toolWindow, DevToolsIdeFeature ideFeature) {
    // Consulo: no JxBrowser; use the embedded browser when one is available, otherwise open DevTools in the external browser.
    presentDevTools(app, toolWindow, FlutterUtils.embeddedBrowser(myProject) != null, ideFeature);
  }

  private void updateForEmptyContent(@NotNull ToolWindow toolWindow) {
    // There's a possible race here where the tool window gets disposed while we're displaying contents.
    if (toolWindow.isDisposed()) {
      return;
    }

    // Display a 'No running applications' message.
    final ContentManager contentManager = toolWindow.getContentManager();
    final JPanel panel = new JPanel(new BorderLayout());
    final JBLabel label = new JBLabel("No running applications", SwingConstants.CENTER);
    label.setForeground(UIUtil.getLabelDisabledForeground());
    panel.add(label, BorderLayout.CENTER);
    emptyContent = contentManager.getFactory().createContent(panel, null, false);
    contentManager.addContent(emptyContent);
  }

  // Returns true if the toolWindow was initially closed but opened automatically on app launch.
  private @Nullable DevToolsIdeFeature updateToolWindowVisibility(@NotNull ToolWindow flutterToolWindow) {
    if (flutterToolWindow.isVisible()) {
      return DevToolsIdeFeature.TOOL_WINDOW_RELOAD;
    }

    if (FlutterSettings.getInstance().isOpenInspectorOnAppLaunch()) {
      flutterToolWindow.show(null);
      return DevToolsIdeFeature.ON_DEBUG_AUTOMATIC;
    }

    return null;
  }
}
