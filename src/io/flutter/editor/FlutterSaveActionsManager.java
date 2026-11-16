/*
 * Copyright 2018 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.editor;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import jakarta.inject.Singleton;
import jakarta.inject.Inject;

import consulo.language.codeStyle.CodeStyle;
import consulo.language.editor.WriteCommandAction;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.event.FileDocumentManagerListener;
import consulo.module.Module;
import consulo.ide.impl.idea.openapi.module.ModuleUtil;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.editor.PsiErrorElementUtil;
import consulo.component.messagebus.MessageBus;
import consulo.component.messagebus.MessageBusConnection;
import com.jetbrains.lang.dart.DartLanguage;
import com.jetbrains.lang.dart.analyzer.DartAnalysisServerService;
import com.jetbrains.lang.dart.assists.AssistUtils;
import io.flutter.FlutterUtils;
import io.flutter.dart.DartPlugin;
import io.flutter.settings.FlutterSettings;
import io.flutter.utils.OpenApiUtils;
import org.dartlang.analysis.server.protocol.SourceEdit;
import org.dartlang.analysis.server.protocol.SourceFileEdit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * A manager class to run actions on save (formatting, organize imports, ...).
 */
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
public class FlutterSaveActionsManager {

  /**
   * Initialize the save actions manager for the given project.
   */
  public static void init(@NotNull Project project) {
    // Call getInstance() will init FlutterSaveActionsManager for the given project by calling the private constructor below.
    getInstance(project);
  }

  @Nullable
  public static FlutterSaveActionsManager getInstance(@NotNull Project project) {
    return project.getInstance(FlutterSaveActionsManager.class);
  }

  private final @NotNull Project myProject;
  @Inject
  public FlutterSaveActionsManager(@NotNull Project project) {
    this.myProject = project;

    final MessageBus bus = project.getMessageBus();
    final MessageBusConnection connection = bus.connect();
    connection.subscribe(FileDocumentManagerListener.class, new FileDocumentManagerListener() {
      @Override
      public void beforeDocumentSaving(@NotNull Document document) {
        // Don't try and format read only docs.
        if (!document.isWritable()) {
          return;
        }

        handleBeforeDocumentSaving(document);
      }
    });
  }

  private void handleBeforeDocumentSaving(@NotNull Document document) {
    final FlutterSettings settings = FlutterSettings.getInstance();
    if (!settings.isFormatCodeOnSave()) {
      return;
    }

    if (!myProject.isInitialized() || myProject.isDisposed()) {
      return;
    }

    final VirtualFile file = FileDocumentManager.getInstance().getFile(document);
    if (file == null) {
      return;
    }

    if (!FlutterUtils.isDartFile(file)) {
      return;
    }

    final PsiFile psiFile = PsiDocumentManager.getInstance(myProject).getPsiFile(document);
    if (psiFile == null || !psiFile.isValid()) {
      return;
    }

    final Module module = ModuleUtil.findModuleForFile(file, myProject);
    if (module == null) {
      return;
    }

    if (!DartPlugin.isDartSdkEnabled(module)) {
      return;
    }

    // check for errors
    if (PsiErrorElementUtil.hasErrors(myProject, psiFile.getVirtualFile())) {
      return;
    }

    if (DartAnalysisServerService.getInstance(myProject).serverReadyForRequest()) {
      if (settings.isOrganizeImportsOnSave()) {
        performOrganizeThenFormat(document, file, psiFile);
      }
      else {
        performFormat(document, file, false, psiFile);
      }
    }
  }

  private void performOrganizeThenFormat(@NotNull Document document, @NotNull VirtualFile file, @NotNull PsiFile psiFile) {
    final String filePath = file.getPath();
    final SourceFileEdit fileEdit = DartAnalysisServerService.getInstance(myProject).edit_organizeDirectives(filePath);

    if (fileEdit != null) {
      OpenApiUtils.safeInvokeLater(() -> {
        if (myProject.isDisposed()) {
          return;
        }

        WriteCommandAction.writeCommandAction(myProject).run(() -> {
          if (myProject.isDisposed()) {
            return;
          }

          AssistUtils.applySourceEdits(myProject, file, document, fileEdit.getEdits(), Collections.emptySet());

          // Committing a document here is required in order to guarantee that DartPostFormatProcessor.processText() is called afterwards.
          PsiDocumentManager.getInstance(myProject).commitDocument(document);

          // Run this in an invoke later so that we don't exeucte the initial part of performFormat in a write action.
          //noinspection CodeBlock2Expr
          OpenApiUtils.safeInvokeLater(() -> {
            performFormat(document, file, true, psiFile);
          });
        });
      });
    }
  }

  private void performFormat(@NotNull Document document, @NotNull VirtualFile file, boolean reSave, @NotNull PsiFile psiFile) {
    final int lineLength = getRightMargin(psiFile);
    final DartAnalysisServerService das = DartAnalysisServerService.getInstance(myProject);

    das.updateFilesContent();

    final DartAnalysisServerService.FormatResult formatResult = das.edit_format(file, 0, 0, lineLength);

    if (formatResult == null) {
      if (reSave) {
        FileDocumentManager.getInstance().saveDocument(document);
      }
      return;
    }

    OpenApiUtils.safeInvokeLater(() -> {
      if (myProject.isDisposed()) {
        return;
      }

      WriteCommandAction.writeCommandAction(myProject).run(() -> {
        if (myProject.isDisposed()) {
          return;
        }

        boolean didFormat = false;

        final List<SourceEdit> edits = formatResult.getEdits();
        if (edits != null && edits.size() == 1) {
          final String replacement = StringUtil.convertLineSeparators(edits.get(0).getReplacement());
          document.replaceString(0, document.getTextLength(), replacement);
          PsiDocumentManager.getInstance(myProject).commitDocument(document);

          didFormat = true;
        }

        // Don't perform the save in a write action - it could invoke EDT work.
        if (reSave || didFormat) {
          //noinspection CodeBlock2Expr
          OpenApiUtils.safeInvokeLater(() -> {
            FileDocumentManager.getInstance().saveDocument(document);
          });
        }
      });
    });
  }

  private static int getRightMargin(@NotNull PsiFile psiFile) {
    return CodeStyle.getLanguageSettings(psiFile, DartLanguage.INSTANCE).RIGHT_MARGIN;
  }
}
