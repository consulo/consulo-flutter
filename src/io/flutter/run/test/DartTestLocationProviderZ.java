/*
 * Copyright 2020 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.run.test;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import consulo.execution.action.Location;
import consulo.execution.action.PsiLocation;
import consulo.execution.test.sm.runner.SMTestLocator;
import consulo.document.Document;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.resolve.PsiElementProcessor;
import consulo.language.psi.util.PsiTreeUtil;
import com.jetbrains.lang.dart.ide.runner.util.TestUtil;
import com.jetbrains.lang.dart.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class DartTestLocationProviderZ implements SMTestLocator, DumbAware {
  @SuppressWarnings("rawtypes")
  private static final List<Location> NONE = Collections.emptyList();
  private static final Gson GSON = new Gson();

  public static final DartTestLocationProviderZ INSTANCE = new DartTestLocationProviderZ();

  public static final Type STRING_LIST_TYPE = new TypeToken<List<String>>() {
  }.getType();

  @NotNull
  @Override
  @SuppressWarnings("rawtypes")
  public List<Location> getLocation(@NotNull String protocol,
                                    @NotNull String path,
                                    @NotNull Project project,
                                    @NotNull GlobalSearchScope scope) {
    // see DartTestEventsConverterZ.addLocationHint()
    // path is like /Users/x/projects/foo/test/foo_test.dart,35,12,["main tests","calculate_fail"]

    final int commaIdx1 = path.indexOf(',');
    final int commaIdx2 = path.indexOf(',', commaIdx1 + 1);
    final int commaIdx3 = path.indexOf(',', commaIdx2 + 1);
    if (commaIdx3 < 0) return NONE;

    final String filePath = path.substring(0, commaIdx1);
    final int line = Integer.parseInt(path.substring(commaIdx1 + 1, commaIdx2));
    final int column = Integer.parseInt(path.substring(commaIdx2 + 1, commaIdx3));
    final String names = path.substring(commaIdx3 + 1);

    final VirtualFile file = LocalFileSystem.getInstance().findFileByPath(filePath);
    final PsiFile psiFile = file == null ? null : PsiManager.getInstance(project).findFile(file);
    if (!(psiFile instanceof DartFile)) return NONE;

    if (line >= 0 && column >= 0) {
      final Location<PsiElement> location = getLocationByLineAndColumn(psiFile, line, column);
      if (location != null) {
        return Collections.singletonList(location);
      }
    }

    final List<String> nodes = pathToNodes(names);
    if (nodes.isEmpty()) {
      return Collections.singletonList(new PsiLocation<PsiElement>(psiFile));
    }

    return getLocationByGroupAndTestNames(psiFile, nodes);
  }

  @Nullable
  protected Location<PsiElement> getLocationByLineAndColumn(@NotNull final PsiFile file, final int line, final int column) {
    final Document document = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
    if (document == null) return null;
    if (line >= document.getLineCount()) return null;

    final int offset = document.getLineStartOffset(line) + column;
    final PsiElement element = file.findElementAt(offset);
    final PsiElement parent1 = element == null ? null : element.getParent();
    final PsiElement parent2 = parent1 instanceof DartId ? parent1.getParent() : null;
    final PsiElement parent3 = parent2 instanceof DartReferenceExpression ? parent2.getParent() : null;
    if (parent3 instanceof DartCallExpression) {
      if (TestUtil.isTest((DartCallExpression)parent3) || TestUtil.isGroup((DartCallExpression)parent3)) {
        return new PsiLocation<>(parent3);
      }
    }
    return null;
  }

  private static List<String> pathToNodes(final String element) {
    return GSON.fromJson(element, STRING_LIST_TYPE);
  }

  @VisibleForTesting
  @SuppressWarnings("rawtypes")
  public List<Location> getLocationForTest(@NotNull final PsiFile psiFile, @NotNull final String testPath) {
    return getLocationByGroupAndTestNames(psiFile, pathToNodes(testPath));
  }

  @SuppressWarnings("rawtypes")
  protected List<Location> getLocationByGroupAndTestNames(final PsiFile psiFile, final List<String> nodes) {
    final List<Location> locations = new ArrayList<>();

    if (psiFile instanceof DartFile && !nodes.isEmpty()) {
      final PsiElementProcessor<PsiElement> collector = new PsiElementProcessor<>() {
        @Override
        public boolean execute(@NotNull final PsiElement element) {
          if (element instanceof DartCallExpression expression) {
            if (isTest(expression) || TestUtil.isGroup(expression)) {
              if (Objects.equals(nodes.get(nodes.size() - 1), getTestLabel(expression))) {
                boolean matches = true;
                for (int i = nodes.size() - 2; i >= 0 && matches; --i) {
                  expression = getGroup(expression);
                  if (expression == null || !Objects.equals(nodes.get(i), getTestLabel(expression))) {
                    matches = false;
                  }
                }
                if (matches) {
                  locations.add(new PsiLocation<>(element));
                  return false;
                }
              }
            }
          }

          return true;
        }

        @Nullable
        private DartCallExpression getGroup(final DartCallExpression expression) {
          return (DartCallExpression)PsiTreeUtil.findFirstParent(expression, true,
                                                                 element -> element instanceof DartCallExpression &&
                                                                            TestUtil.isGroup((DartCallExpression)element));
        }
      };

      PsiTreeUtil.processElements(psiFile, collector);
    }

    return locations;
  }

  protected boolean isTest(@NotNull DartCallExpression expression) {
    return TestUtil.isTest(expression);
  }

  @Nullable
  public static String getTestLabel(@NotNull final DartCallExpression testCallExpression) {
    final DartArguments arguments = testCallExpression.getArguments();
    final DartArgumentList argumentList = arguments == null ? null : arguments.getArgumentList();
    final List<DartExpression> argExpressions = argumentList == null ? null : argumentList.getExpressionList();
    return argExpressions != null && !argExpressions.isEmpty() && argExpressions.get(0) instanceof DartStringLiteralExpression
           ? StringUtil.unquoteString(argExpressions.get(0).getText())
           : null;
  }
}
