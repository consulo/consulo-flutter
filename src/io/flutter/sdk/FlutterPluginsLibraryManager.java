/*
 * Copyright 2017 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.sdk;

import consulo.application.ApplicationManager;
import consulo.ui.ModalityState;
import consulo.application.ReadAction;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.module.content.layer.event.ModuleRootEvent;
import consulo.module.content.layer.event.ModuleRootListener;
import consulo.content.library.PersistentLibraryKind;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.VirtualFileEvent;
import consulo.virtualFileSystem.event.VirtualFileListener;
import consulo.virtualFileSystem.event.VirtualFileMoveEvent;
import consulo.virtualFileSystem.event.VirtualFilePropertyEvent;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.application.util.concurrent.AppExecutorUtil;
import io.flutter.FlutterProjectDisposable;
import io.flutter.pub.PubRoot;
import io.flutter.pub.PubRoots;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages the Flutter Plugins library, which hooks the packages used by plugins referenced in a project
 * into the project, so full editing support is available.
 *
 * @see FlutterPluginLibraryType
 * @see FlutterPluginLibraryProperties
 */
public class FlutterPluginsLibraryManager extends AbstractLibraryManager<FlutterPluginLibraryProperties> {

  private final AtomicBoolean isUpdating = new AtomicBoolean(false);

  public FlutterPluginsLibraryManager(@NotNull Project project) {
    super(project);
  }

  public void startWatching() {
    var project = getProject();
    // Consulo: VirtualFileContentsChangedAdapter does not exist; the listener reacts to the same "after" events
    VirtualFileManager.getInstance().addVirtualFileListener(new VirtualFileListener() {
      @Override
      public void propertyChanged(@NotNull VirtualFilePropertyEvent event) {
        fileChanged(getProject(), event.getFile());
      }

      @Override
      public void contentsChanged(@NotNull VirtualFileEvent event) {
        fileChanged(getProject(), event.getFile());
      }

      @Override
      public void fileCreated(@NotNull VirtualFileEvent event) {
        fileChanged(getProject(), event.getFile());
      }

      @Override
      public void fileDeleted(@NotNull VirtualFileEvent event) {
        fileChanged(getProject(), event.getFile());
      }

      @Override
      public void fileMoved(@NotNull VirtualFileMoveEvent event) {
        fileChanged(getProject(), event.getFile());
      }
    }, FlutterProjectDisposable.getInstance(project));

    project.getMessageBus().connect().subscribe(ModuleRootListener.class, new ModuleRootListener() {
      @Override
      public void rootsChanged(@NotNull ModuleRootEvent event) {
        scheduleUpdate();
      }
    });

    scheduleUpdate();
  }

  @Override
  @NotNull
  protected String getLibraryName() {
    return FlutterPluginLibraryType.FLUTTER_PLUGINS_LIBRARY_NAME;
  }

  @Override
  @NotNull
  protected PersistentLibraryKind<FlutterPluginLibraryProperties> getLibraryKind() {
    return FlutterPluginLibraryType.LIBRARY_KIND;
  }
  
  private void fileChanged(@NotNull final Project project, @NotNull final VirtualFile file) {
    if (LocalFileSystem.getInstance() != file.getFileSystem() && !ApplicationManager.getApplication().isUnitTestMode()) return;

    scheduleUpdate();
  }

  private void scheduleUpdate() {
    if (isUpdating.get()) {
      return;
    }

    final Runnable runnable = this::updateFlutterPlugins;
    DumbService.getInstance(getProject()).smartInvokeLater(runnable, ModalityState.nonModal());
  }

  private void updateFlutterPlugins() {
    if (!isUpdating.compareAndSet(false, true)) {
      return;
    }

    try {
      updateFlutterPluginsImpl();
    }
    finally {
      isUpdating.set(false);
    }
  }

  private void updateFlutterPluginsImpl() {
    Project project = getProject();

    ReadAction.nonBlocking(() -> getFlutterPluginPaths(PubRoots.forProject(project)))
      .expireWith(FlutterProjectDisposable.getInstance(project))
      .coalesceBy(this)
      .finishOnUiThread(application -> ModalityState.nonModal(), flutterPluginPaths -> {
        final Set<String> flutterPluginUrls = new HashSet<>();
        for (String path : flutterPluginPaths) {
          flutterPluginUrls.add(VirtualFileUtil.pathToUrl(path));
        }
        updateLibraryContent(flutterPluginUrls);
      })
      .submit(AppExecutorUtil.getAppExecutorService());
  }

  private static @NotNull Set<@NotNull String> getFlutterPluginPaths(@NotNull List<@NotNull PubRoot> roots) {
    final Set<String> paths = new HashSet<>();

    for (PubRoot pubRoot : roots) {
      final var packagesMap = pubRoot.getPackagesMap();
      if (packagesMap == null) {
        continue;
      }

      for (String packagePath : packagesMap.values()) {
        if (packagePath == null) continue;
        final VirtualFile libFolder = LocalFileSystem.getInstance().findFileByPath(packagePath);
        if (libFolder == null) {
          continue;
        }
        final PubRoot pluginRoot = PubRoot.forDirectory(libFolder.getParent());
        if (pluginRoot == null) {
          continue;
        }

        if (pluginRoot.isFlutterPlugin()) {
          paths.add(pluginRoot.getPath());
        }
      }
    }

    return paths;
  }
}
