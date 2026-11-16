/*
 * Copyright 2019 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.actions;

import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionRefAnchor;
import consulo.flutter.actions.FlutterBuildGroup;
import consulo.annotation.component.ActionImpl;
import consulo.ui.ex.action.AnSeparator;
import consulo.flutter.localize.FlutterConsuloLocalize;

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

@ActionImpl(id = "FlutterBuildActionGroup", children = {
  @ActionRef(type = AnSeparator.class),
  @ActionRef(type = FlutterBuildGroup.class)
}, parents = @ActionParentRef(value = @ActionRef(id = "BuildMenu"), anchor = ActionRefAnchor.FIRST))
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

  @ActionImpl(id = "flutter.build.aar")
  public static class AAR extends FlutterBuildAction {
    public AAR() {
      // Consulo: text, description and icon of upstream plugin.xml <action>
      getTemplatePresentation().setText(FlutterConsuloLocalize.actionFlutterBuildAarText());
      getTemplatePresentation().setDescription(FlutterConsuloLocalize.actionFlutterBuildAarDescription());
    }

    @Override
    protected @NotNull BuildType buildType() {
      return BuildType.AAR;
    }
  }

  @ActionImpl(id = "flutter.build.apk")
  public static class APK extends FlutterBuildAction {
    public APK() {
      // Consulo: text, description and icon of upstream plugin.xml <action>
      getTemplatePresentation().setText(FlutterConsuloLocalize.actionFlutterBuildApkText());
      getTemplatePresentation().setDescription(FlutterConsuloLocalize.actionFlutterBuildApkDescription());
    }

    @Override
    protected @NotNull BuildType buildType() {
      return BuildType.APK;
    }
  }

  @ActionImpl(id = "flutter.build.aab")
  public static class AppBundle extends FlutterBuildAction {
    public AppBundle() {
      // Consulo: text, description and icon of upstream plugin.xml <action>
      getTemplatePresentation().setText(FlutterConsuloLocalize.actionFlutterBuildAabText());
      getTemplatePresentation().setDescription(FlutterConsuloLocalize.actionFlutterBuildAabDescription());
    }

    @Override
    protected @NotNull BuildType buildType() {
      return BuildType.APP_BUNDLE;
    }
  }

  @ActionImpl(id = "flutter.build.bundle")
  public static class Bundle extends FlutterBuildAction {
    public Bundle() {
      // Consulo: text, description and icon of upstream plugin.xml <action>
      getTemplatePresentation().setText(FlutterConsuloLocalize.actionFlutterBuildBundleText());
      getTemplatePresentation().setDescription(FlutterConsuloLocalize.actionFlutterBuildBundleDescription());
    }

    @Override
    protected @NotNull BuildType buildType() {
      return BuildType.BUNDLE;
    }
  }

  @ActionImpl(id = "flutter.build.ios")
  public static class Ios extends FlutterBuildAction implements AnActionWithSyncUpdate {
    public Ios() {
      // Consulo: text, description and icon of upstream plugin.xml <action>
      getTemplatePresentation().setText(FlutterConsuloLocalize.actionFlutterBuildIosText());
      getTemplatePresentation().setDescription(FlutterConsuloLocalize.actionFlutterBuildIosDescription());
    }

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

  @ActionImpl(id = "flutter.build.linux")
  public static class Linux extends FlutterBuildAction implements AnActionWithSyncUpdate {
    public Linux() {
      // Consulo: text, description and icon of upstream plugin.xml <action>
      getTemplatePresentation().setText(FlutterConsuloLocalize.actionFlutterBuildLinuxText());
      getTemplatePresentation().setDescription(FlutterConsuloLocalize.actionFlutterBuildLinuxDescription());
    }

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

  @ActionImpl(id = "flutter.build.macos")
  public static class Macos extends FlutterBuildAction implements AnActionWithSyncUpdate {
    public Macos() {
      // Consulo: text, description and icon of upstream plugin.xml <action>
      getTemplatePresentation().setText(FlutterConsuloLocalize.actionFlutterBuildMacosText());
      getTemplatePresentation().setDescription(FlutterConsuloLocalize.actionFlutterBuildMacosDescription());
    }

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

  @ActionImpl(id = "flutter.build.web")
  public static class Web extends FlutterBuildAction {
    public Web() {
      // Consulo: text, description and icon of upstream plugin.xml <action>
      getTemplatePresentation().setText(FlutterConsuloLocalize.actionFlutterBuildWebText());
      getTemplatePresentation().setDescription(FlutterConsuloLocalize.actionFlutterBuildWebDescription());
    }

    @Override
    protected @NotNull BuildType buildType() {
      return BuildType.WEB;
    }
  }

  @ActionImpl(id = "flutter.build.windows")
  public static class Windows extends FlutterBuildAction implements AnActionWithSyncUpdate {
    public Windows() {
      // Consulo: text, description and icon of upstream plugin.xml <action>
      getTemplatePresentation().setText(FlutterConsuloLocalize.actionFlutterBuildWindowsText());
      getTemplatePresentation().setDescription(FlutterConsuloLocalize.actionFlutterBuildWindowsDescription());
    }

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
