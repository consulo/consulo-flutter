/*
 * Copyright 2019 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.actions;

import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.process.ProcessHandler;
import consulo.process.event.ProcessEvent;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.Presentation;
import consulo.module.Module;
import consulo.language.util.ModuleUtilCore;
import consulo.project.Project;
import consulo.application.util.SystemInfo;
import consulo.virtualFileSystem.VirtualFile;
import io.flutter.FlutterMessages;
import io.flutter.pub.PubRoot;
import io.flutter.pub.PubRoots;
import io.flutter.sdk.FlutterSdk;
import io.flutter.utils.FlutterModuleUtils;
import io.flutter.utils.OpenApiUtils;
import io.flutter.utils.ProcessAdapter;
import io.flutter.utils.ProgressHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FlutterBuildActionGroup extends DefaultActionGroup implements AnActionWithSyncUpdate {

  public static void build(@NotNull Project project,
                           @NotNull PubRoot pubRoot,
                           @NotNull FlutterSdk sdk,
                           @NotNull BuildType buildType,
                           @Nullable String desc) {
    final ProgressHelper progressHelper = new ProgressHelper(project);
    progressHelper.start(desc == null ? "building" : desc);
    ProcessAdapter processAdapter = new ProcessAdapter() {
      @Override
      public void processTerminated(@NotNull ProcessEvent event) {
        progressHelper.done();
        final int exitCode = event.getExitCode();
        if (exitCode != 0) {
          FlutterMessages.showError("Error while building " + buildType, "`flutter build` returned: " + exitCode, project);
        }
      }
    };
    final Module module = pubRoot.getModule(project);
    if (module != null) {
      sdk.flutterBuild(pubRoot, buildType.type).startInModuleConsole(module, pubRoot::refresh, processAdapter);
    }
    else {
      final ProcessHandler processHandler = sdk.flutterBuild(pubRoot, buildType.type).startInConsole(project);
      if (processHandler == null) {
        progressHelper.done();
      }
      else {
        processHandler.addProcessListener(processAdapter);
      }
    }
  }

  public enum BuildType {
    AAR("aar"),
    APK("apk"),
    APP_BUNDLE("appbundle"),
    BUNDLE("bundle"),
    IOS("ios"),
    LINUX("linux"),
    MACOS("macos"),
    WEB("web"),
    WINDOWS("windows");

    final public String type;

    BuildType(String type) {
      this.type = type;
    }
  }

  @Override
  public void update(@NotNull AnActionEvent event) {
    final Presentation presentation = event.getPresentation();
    final boolean enabled = isInFlutterModule(event);
    presentation.setEnabled(enabled);
    presentation.setVisible(enabled);
  }

  private static boolean isInFlutterModule(@NotNull AnActionEvent event) {
    final Project project = event.getData(Project.KEY);
    if (project == null) {
      return false;
    }
    return FlutterModuleUtils.hasFlutterModule(project);
  }

  @Nullable
  public static Module findFlutterModule(@NotNull Project project, @NotNull VirtualFile file) {
    Module module = ModuleUtilCore.findModuleForFile(file, project);
    if (module == null) {
      return null;
    }
    if (FlutterModuleUtils.declaresFlutter(module)) {
      return module;
    }
    // We may get here if the file is in the Android module of a Flutter module project.
    var root = OpenApiUtils.getContentRoots(module)[0];
    if (root == null) return null;

    final VirtualFile parent = root.getParent();
    if (parent == null) return null;

    module = ModuleUtilCore.findModuleForFile(parent, project);
    if (module == null) {
      return null;
    }
    return FlutterModuleUtils.declaresFlutter(module) ? module : null;
  }

  abstract public static class FlutterBuildAction extends AnAction {

    public static String ID_PREFIX = "flutter.build";

    @NotNull
    abstract protected BuildType buildType();

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
      final Presentation presentation = event.getPresentation();
      final Project project = event.getData(Project.KEY);
      if (project == null) {
        return;
      }

      final FlutterSdk sdk = FlutterSdk.getFlutterSdk(project);
      if (sdk == null) {
        return;
      }
      final PubRoot pubRoot = PubRoot.forEventWithRefresh(event);
      final BuildType buildType = buildType();
      if (pubRoot != null) {
        build(project, pubRoot, sdk, buildType, presentation.getDescription().get());
      }
      else {
        var roots = PubRoots.forProject(project);
        for (PubRoot sub : roots) {
          build(project, sub, sdk, buildType, presentation.getDescription().get());
        }
      }
    }
  }

  public static class AAR extends FlutterBuildAction {
    @Override
    protected @NotNull BuildType buildType() {
      return BuildType.AAR;
    }
  }

  public static class APK extends FlutterBuildAction {
    @Override
    protected @NotNull BuildType buildType() {
      return BuildType.APK;
    }
  }

  public static class AppBundle extends FlutterBuildAction {
    @Override
    protected @NotNull BuildType buildType() {
      return BuildType.APP_BUNDLE;
    }
  }

  public static class Bundle extends FlutterBuildAction {
    @Override
    protected @NotNull BuildType buildType() {
      return BuildType.BUNDLE;
    }
  }

  public static class Ios extends FlutterBuildAction implements AnActionWithSyncUpdate {
    @Override
    protected @NotNull BuildType buildType() {
      return BuildType.IOS;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
      final Presentation presentation = event.getPresentation();
      presentation.setEnabled(SystemInfo.isMac);
    }
  }

  public static class Linux extends FlutterBuildAction implements AnActionWithSyncUpdate {
    @Override
    protected @NotNull BuildType buildType() {
      return BuildType.LINUX;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
      final Presentation presentation = event.getPresentation();
      presentation.setEnabled(SystemInfo.isLinux);
    }
  }

  public static class Macos extends FlutterBuildAction implements AnActionWithSyncUpdate {
    @Override
    protected @NotNull BuildType buildType() {
      return BuildType.MACOS;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
      final Presentation presentation = event.getPresentation();
      presentation.setEnabled(SystemInfo.isMac);
    }
  }

  public static class Web extends FlutterBuildAction {

    @Override
    protected @NotNull BuildType buildType() {
      return BuildType.WEB;
    }
  }

  public static class Windows extends FlutterBuildAction implements AnActionWithSyncUpdate {
    @Override
    protected @NotNull BuildType buildType() {
      return BuildType.WINDOWS;
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
      final Presentation presentation = event.getPresentation();
      presentation.setEnabled(SystemInfo.isWindows);
    }
  }
}
