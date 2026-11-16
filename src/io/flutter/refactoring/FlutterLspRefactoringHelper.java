/*
 * Copyright 2026 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.refactoring;

import com.intellij.platform.lsp.api.LspServer;
import com.intellij.platform.lsp.api.LspServerManager;
import com.intellij.platform.lsp.api.customization.LspIntentionAction;
import com.intellij.platform.lsp.util.Lsp4jUtilKt;
import com.jetbrains.lang.dart.DartBundle;
import com.jetbrains.lang.dart.lsp.DartLspServerSupportProvider;
import com.jetbrains.lang.dart.sdk.DartConfigurable;
import com.jetbrains.lang.dart.sdk.DartSdk;
import consulo.application.ApplicationManager;
import consulo.application.progress.ProgressManager;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.language.editor.refactoring.RefactoringBundle;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import io.flutter.FlutterBundle;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.CodeActionTriggerKind;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Routes Flutter "Extract Widget" refactoring action to LSP `textDocument/codeAction`
 * requests when LSP code actions are enabled.
 */
// Consulo: converted from a Kotlin object; @JvmStatic/@JvmOverloads members are static overloads, INSTANCE is kept.
public final class FlutterLspRefactoringHelper {
  public static final FlutterLspRefactoringHelper INSTANCE = new FlutterLspRefactoringHelper();

  public static final String EXTRACT_WIDGET_TITLE = "Extract Widget";

  private record CodeActionLookupResult(boolean completed,
                                        boolean serverResponded,
                                        @Nullable LspIntentionAction matchingIntention,
                                        @Nullable String disabledReason) {
  }

  private FlutterLspRefactoringHelper() {
  }

  public static boolean isLspCodeActionsEnabled(@NotNull Project project) {
    try {
      return DartConfigurable.isLspCodeActionsEnabled(project);
    }
    catch (LinkageError e) {
      return false;
    }
  }

  @Nullable
  private static LspServer findDartLspServer(@NotNull Project project) {
    final Iterator<LspServer> servers = LspServerManager.getInstance(project)
      .getServersForProvider(DartLspServerSupportProvider.class)
      .iterator();
    return servers.hasNext() ? servers.next() : null;
  }

  private static int selectionStart(@Nullable Editor editor) {
    return editor != null ? editor.getSelectionModel().getSelectionStart() : 0;
  }

  private static int selectionEnd(@Nullable Editor editor) {
    return editor != null ? editor.getSelectionModel().getSelectionEnd() : 0;
  }

  public static void performExtractWidget(@NotNull Project project, @Nullable Editor editor, @Nullable PsiFile file) {
    performExtractWidget(project, editor, file, selectionStart(editor));
  }

  public static void performExtractWidget(@NotNull Project project, @Nullable Editor editor, @Nullable PsiFile file, int offset) {
    performExtractWidget(project, editor, file, offset, selectionEnd(editor) - offset);
  }

  public static void performExtractWidget(@NotNull Project project,
                                          @Nullable Editor editor,
                                          @Nullable PsiFile file,
                                          int offset,
                                          int length) {
    performExtractWidget(project, editor, file, offset, length, findDartLspServer(project));
  }

  public static void performExtractWidget(@NotNull Project project,
                                          @Nullable Editor editor,
                                          @Nullable PsiFile file,
                                          int offset,
                                          int length,
                                          @Nullable LspServer lspServer) {
    if (editor == null || file == null) return;
    performRefactoring(
      project,
      editor,
      file,
      offset,
      length,
      CodeActionKind.RefactorExtract,
      Set.of(EXTRACT_WIDGET_TITLE),
      FlutterBundle.message("flutter.refactoring.extract.widget.progress"),
      FlutterBundle.message("flutter.refactoring.extract.widget.error"),
      lspServer
    );
  }

  public static void performRefactoring(@NotNull Project project,
                                        @NotNull Editor editor,
                                        @NotNull PsiFile file,
                                        int offset,
                                        int length,
                                        @NotNull String codeActionKind,
                                        @NotNull Set<String> targetTitles,
                                        @NotNull String progressTitle,
                                        @NotNull String errorTitle) {
    performRefactoring(project, editor, file, offset, length, codeActionKind, targetTitles, progressTitle, errorTitle,
                       findDartLspServer(project));
  }

  public static void performRefactoring(@NotNull Project project,
                                        @NotNull Editor editor,
                                        @NotNull PsiFile file,
                                        int offset,
                                        int length,
                                        @NotNull String codeActionKind,
                                        @NotNull Set<String> targetTitles,
                                        @NotNull String progressTitle,
                                        @NotNull String errorTitle,
                                        @Nullable LspServer lspServer) {
    if (DartSdk.getDartSdk(project) == null) return;
    if (!CommonRefactoringUtil.checkReadOnlyStatus(file)) return;
    final VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile == null) return;

    PsiDocumentManager.getInstance(project).commitAllDocuments();

    if (lspServer == null) {
      showCannotRefactorErrorHint(project, editor, errorTitle, DartBundle.message("analysis.server.not.running"));
      return;
    }

    final CodeActionParams params = createCodeActionParams(lspServer, virtualFile, editor.getDocument(), offset, length, codeActionKind);
    final CodeActionLookupResult result = requestMatchingIntention(project, lspServer, params, targetTitles, progressTitle);
    if (!result.completed()) return;
    if (!result.serverResponded()) {
      showCannotRefactorErrorHint(project, editor, errorTitle, FlutterBundle.message("flutter.refactoring.lsp.no.response"));
      return;
    }
    if (result.disabledReason() != null) {
      showCannotRefactorErrorHint(project, editor, errorTitle, result.disabledReason());
      return;
    }
    final LspIntentionAction intentionToInvoke = result.matchingIntention();
    if (intentionToInvoke == null) {
      showCannotRefactorErrorHint(project, editor, errorTitle, null);
      return;
    }
    intentionToInvoke.invoke(virtualFile);
  }

  @NotNull
  private static CodeActionParams createCodeActionParams(@NotNull LspServer lspServer,
                                                         @NotNull VirtualFile virtualFile,
                                                         @NotNull Document document,
                                                         int offset,
                                                         int length,
                                                         @NotNull String codeActionKind) {
    final int textLength = document.getTextLength();
    final int safeOffset = Math.max(0, Math.min(offset, textLength));
    final int safeLength = Math.max(0, Math.min(length, textLength - safeOffset));
    final Range range = Lsp4jUtilKt.getLsp4jRange(document, safeOffset, safeLength);
    final CodeActionContext context = new CodeActionContext(Collections.emptyList(), List.of(codeActionKind));
    context.setTriggerKind(CodeActionTriggerKind.Invoked);
    return new CodeActionParams(lspServer.getDocumentIdentifier(virtualFile), range, context);
  }

  @NotNull
  private static CodeActionLookupResult requestMatchingIntention(@NotNull Project project,
                                                                 @NotNull LspServer lspServer,
                                                                 @NotNull CodeActionParams params,
                                                                 @NotNull Set<String> targetTitles,
                                                                 @NotNull String progressTitle) {
    final boolean[] serverResponded = {false};
    final LspIntentionAction[] matchingIntention = {null};
    final String[] disabledReason = {null};
    final boolean completed = ProgressManager.getInstance().runProcessWithProgressSynchronously(
      () -> {
        final List<Either<Command, CodeAction>> results =
          lspServer.sendRequestSync(it -> it.getTextDocumentService().codeAction(params));
        if (results == null) return;
        serverResponded[0] = true;
        final List<CodeAction> matchingActions = new ArrayList<>();
        for (Either<Command, CodeAction> result : results) {
          final CodeAction action = result.getRight();
          if (action != null && targetTitles.contains(action.getTitle())) {
            matchingActions.add(action);
          }
        }
        CodeAction validAction = null;
        for (CodeAction action : matchingActions) {
          if (action.getDisabled() == null) {
            validAction = action;
            break;
          }
        }
        if (validAction != null) {
          final LspIntentionAction intention = new LspIntentionAction(lspServer, validAction);
          ApplicationManager.getApplication().runReadAction(() -> {
            if (intention.isAvailable()) {
              matchingIntention[0] = intention;
            }
          });
        }
        else {
          for (CodeAction action : matchingActions) {
            if (action.getDisabled() != null) {
              disabledReason[0] = action.getDisabled().getReason();
              break;
            }
          }
        }
      },
      // Consulo: progress titles are LocalizeValue
      LocalizeValue.of(progressTitle),
      true,
      project
    );
    return new CodeActionLookupResult(completed, serverResponded[0], matchingIntention[0], disabledReason[0]);
  }

  private static void showCannotRefactorErrorHint(@NotNull Project project,
                                                  @NotNull Editor editor,
                                                  @NotNull String errorTitle,
                                                  @Nullable String reason) {
    final String message = RefactoringBundle.getCannotRefactorMessage(reason);
    CommonRefactoringUtil.showErrorHint(project, editor, LocalizeValue.of(message), LocalizeValue.of(errorTitle), null);
  }
}
