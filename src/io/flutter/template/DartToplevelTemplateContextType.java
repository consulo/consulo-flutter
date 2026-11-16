/*
 * Copyright 2017 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.template;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import com.jetbrains.lang.dart.ide.template.DartTemplateContextType;
import com.jetbrains.lang.dart.psi.DartClassDefinition;
import consulo.localize.LocalizeValue;
import org.jetbrains.annotations.NotNull;

// Consulo: registered here instead of the upstream plugin.xml liveTemplateContext
@ExtensionImpl
public class DartToplevelTemplateContextType extends DartTemplateContextType {
  public DartToplevelTemplateContextType() {
    super("DART_TOPLEVEL", LocalizeValue.localizeTODO("Top-level"), Generic.class);
  }

  @Override
  protected boolean isInContext(@NotNull PsiElement element) {
    return PsiTreeUtil.getNonStrictParentOfType(element, DartClassDefinition.class, PsiComment.class) == null;
  }
}
