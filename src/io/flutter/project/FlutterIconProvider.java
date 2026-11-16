/*
 * Copyright 2017 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.project;

import consulo.annotation.component.ExtensionImpl;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.AllIcons;
import consulo.component.util.Iconable.IconFlags;
import consulo.language.icon.IconDescriptor;
import consulo.language.icon.IconDescriptorUpdater;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.virtualFileSystem.VirtualFile;
import com.jetbrains.lang.dart.DartFileType;
import com.jetbrains.lang.dart.psi.DartFile;
import io.flutter.icons.FlutterIcons;
import io.flutter.FlutterUtils;
import io.flutter.pub.PubRoot;
import io.flutter.pub.PubRootCache;
import io.flutter.utils.FlutterModuleUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

// Consulo: upstream IconProvider is an IconDescriptorUpdater; the icon computed by getIcon() replaces the main icon
@ExtensionImpl(order = "first")
public class FlutterIconProvider implements IconDescriptorUpdater {
  @SuppressWarnings("DataFlowIssue") private static final @NotNull Image TEST_FILE =
    overlayIcons(DartFileType.INSTANCE.getIcon(), AllIcons.Nodes.JunitTestMark);

  @RequiredReadAction
  @Override
  public void updateIcon(@NotNull IconDescriptor iconDescriptor, @NotNull PsiElement element, int flags) {
    final Image icon = getIcon(element, flags);
    if (icon != null) {
      iconDescriptor.setMainIcon(icon);
    }
  }

  @Nullable
  public Image getIcon(@NotNull PsiElement element, @IconFlags int flags) {
    final Project project = element.getProject();
    if (!FlutterModuleUtils.declaresFlutter(project)) return null;

    // Directories.
    if (element instanceof PsiDirectory) {
      final VirtualFile dir = ((PsiDirectory)element).getVirtualFile();
      if (!dir.isInLocalFileSystem()) return null;

      final PubRootCache pubRootCache = PubRootCache.getInstance(project);

      // Show an icon for flutter modules.
      final PubRoot pubRoot = pubRootCache.getRoot(dir);
      if (pubRoot != null && dir.equals(pubRoot.getRoot()) && pubRoot.declaresFlutter()) {
        return FlutterIcons.Flutter;
      }

      final PubRoot root = pubRootCache.getRoot(dir.getParent());
      if (root == null) return null;

      // TODO(devoncarew): should we just make the folder a source kind?
      if (dir.equals(root.getLib())) return AllIcons.Modules.SourceRoot;

      if (Objects.equals(dir, root.getAndroidDir())) return AllIcons.Nodes.KeymapTools;
      if (Objects.equals(dir, root.getiOsDir())) return AllIcons.Nodes.KeymapTools;
      if (Objects.equals(dir, root.getIntegrationTestDir())) return PlatformIconGroup.modulesTestroot();

      if (dir.isDirectory() && dir.getName().equals(".idea")) return PlatformIconGroup.modulesGeneratedroot();
    }

    // Files.
    if (element instanceof DartFile dartFile) {
      final VirtualFile file = dartFile.getVirtualFile();
      if (file == null || !file.isInLocalFileSystem()) return null;

      // Use a simple naming convention heuristic to identify test files.
      // TODO(pq): consider pushing up to the Dart Plugin.
      if (FlutterUtils.isInTestDir(dartFile) && file.getName().endsWith("_test.dart")) {
        return TEST_FILE;
      }
    }

    return null;
  }

  @NotNull
  private static Image overlayIcons(@NotNull Image @NotNull ... icons) {
    return ImageEffects.layered(icons);
  }
}
