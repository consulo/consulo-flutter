/*
 * Copyright 2018 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.samples;

import consulo.annotation.component.ExtensionImpl;
import jakarta.inject.Inject;

import com.google.common.annotations.VisibleForTesting;
import consulo.webBrowser.BrowserUtil;
import consulo.document.Document;
import consulo.codeEditor.Editor;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.TextEditor;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.fileEditor.EditorNotificationBuilder;
import consulo.fileEditor.EditorNotificationProvider;
import consulo.localize.LocalizeValue;
import com.jetbrains.lang.dart.psi.DartClass;
import io.flutter.icons.FlutterIcons;
import io.flutter.sdk.FlutterSdk;
import io.flutter.utils.OpenApiUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;

@ExtensionImpl
public class FlutterSampleNotificationProvider implements EditorNotificationProvider {
  @NotNull final Project project;

  @Inject
  public FlutterSampleNotificationProvider(@NotNull Project project) {
    this.project = project;
  }

  @NotNull
  @Override
  public String getId() {
    return "flutter.sample.notifications";
  }

  // Consulo: EditorNotificationProvider fills an EditorNotificationBuilder instead of returning a panel factory
  @RequiredReadAction
  @Nullable
  @Override
  public EditorNotificationBuilder buildNotification(@NotNull VirtualFile file,
                                                     @NotNull FileEditor fileEditor,
                                                     @NotNull Supplier<EditorNotificationBuilder> builderFactory) {
    final FlutterSdk sdk = FlutterSdk.getFlutterSdk(project);
    if (sdk == null) {
      return null;
    }

    final String flutterPackagePath = sdk.getHomePath() + "/packages/flutter/lib/src/";

    // Only show for files in the flutter sdk.
    final String filePath = file.getPath();
    if (!filePath.startsWith(flutterPackagePath)) {
      return null;
    }

    return createPanelForSamples(fileEditor, project, file, filePath, sdk, flutterPackagePath, builderFactory);
  }

  @Nullable
  private EditorNotificationBuilder createPanelForSamples(@NotNull FileEditor fileEditor,
                                                        @NotNull Project project,
                                                        @NotNull VirtualFile file,
                                                        @NotNull String filePath,
                                                        @NotNull FlutterSdk sdk,
                                                        @NotNull String flutterPackagePath,
                                                        @NotNull Supplier<EditorNotificationBuilder> builderFactory) {
    if (!(fileEditor instanceof TextEditor textEditor)) {
      return null;
    }

    final Editor editor = textEditor.getEditor();
    final Document document = editor.getDocument();
    final PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(project);
    if (psiDocumentManager == null) {
      return null;
    }

    final PsiFile psiFile = psiDocumentManager.getPsiFile(document);
    if (psiFile == null || !psiFile.isValid()) {
      return null;
    }

    // Run the code to query the document in a read action.
    final List<FlutterSample> samples = OpenApiUtils.safeRunReadAction(() -> {
      return getSamplesFromDoc(flutterPackagePath, document, filePath);
    });

    if (samples != null && !samples.isEmpty()) {
      return FlutterSampleActionsPanel.fill(builderFactory.get(), samples);
    }
    return null;
  }

  @NotNull
  private List<FlutterSample> getSamplesFromDoc(@NotNull String flutterPackagePath, @NotNull Document document, @NotNull String filePath) {
    final List<FlutterSample> samples = new ArrayList<>();

    PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
    if (documentManager == null) return samples;

    // Find all candidate class definitions.
    final PsiFile psiFile = documentManager.getPsiFile(document);
    assert (psiFile != null);

    final DartClass[] classes = PsiTreeUtil.getChildrenOfType(psiFile, DartClass.class);
    if (classes == null) {
      return Collections.emptyList();
    }

    // Get the dartdoc for the classes and use a regex to identify which ones have
    // "/// {@tool dartpad ...}".

    for (DartClass declaration : classes) {
      if (declaration == null) continue;
      final String name = declaration.getName();
      if (name == null || name.startsWith("_")) {
        continue;
      }

      List<String> dartdoc = null;
      try {
        // Context: https://github.com/flutter/flutter-intellij/issues/5634
        dartdoc = DartDocumentUtils.getDartdocFor(document, declaration);
      }
      catch (IndexOutOfBoundsException e) {
        // ignore
      }
      if (dartdoc != null && containsDartdocFlutterSample(dartdoc)) {
        assert (declaration.getName() != null);

        String libraryName = filePath.substring(flutterPackagePath.length());
        final int index = libraryName.indexOf('/');
        if (index != -1) {
          libraryName = libraryName.substring(0, index);

          final FlutterSample sample = new FlutterSample(libraryName, declaration.getName());
          samples.add(sample);
        }
      }
    }
    return samples;
  }

  // "/// {@tool dartpad ...}"
  private static final Pattern DARTPAD_TOOL_PATTERN = Pattern.compile("\\{@tool.*\\sdartpad.*}");

  /**
   * Return whether the given lines of dartdoc text contain a reference to an embedded dartpad Flutter
   * widget sample, eg. <code>"/// {\@tool dartpad ...}"</code>(.
   */
  @VisibleForTesting
  public static boolean containsDartdocFlutterSample(@NotNull List<String> lines) {
    if (lines.isEmpty()) {
      return false;
    }

    for (String line : lines) {
      if (line != null && DARTPAD_TOOL_PATTERN.matcher(line).find()) {
        return true;
      }
    }

    return false;
  }
}

// Consulo: the notification panel is built by EditorNotificationBuilder; there are no separators between the links
final class FlutterSampleActionsPanel {
  private FlutterSampleActionsPanel() {
  }

  @NotNull
  static EditorNotificationBuilder fill(@NotNull EditorNotificationBuilder builder, @NotNull List<FlutterSample> samples) {
    builder.withIcon(FlutterIcons.Flutter);
    builder.withText(LocalizeValue.localizeTODO("View example on flutter.dev"));

    for (final FlutterSample sample : samples) {
      assert sample != null;

      builder.withAction(LocalizeValue.of(sample.getClassName()), LocalizeValue.of(sample.getHostedDocsUrl()), e -> browseTo(sample));
    }
    return builder;
  }

  private static void browseTo(@NotNull FlutterSample sample) {
    BrowserUtil.browse(sample.getHostedDocsUrl());
  }
}
