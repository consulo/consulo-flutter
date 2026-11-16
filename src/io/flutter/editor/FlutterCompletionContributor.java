/*
 * Copyright 2026 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package io.flutter.editor;

import consulo.annotation.component.ExtensionImpl;

import com.jetbrains.lang.dart.DartLanguage;
import com.jetbrains.lang.dart.psi.DartReferenceExpression;
import consulo.language.Language;
import consulo.language.editor.completion.CompletionContributor;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupElementDecorator;
import consulo.language.editor.completion.lookup.LookupElementPresentation;
import consulo.language.editor.completion.lookup.LookupElementRenderer;
import consulo.language.psi.PsiElement;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

// Consulo: converted from Kotlin; icons are consulo.ui.image.Image (ColorIcon/EmptyIcon -> ImageEffects).
@ExtensionImpl(order = "first")
public class FlutterCompletionContributor extends CompletionContributor {
  private static final int ICON_SIZE = 16;
  private static final Image EMPTY_ICON = ImageEffects.empty(ICON_SIZE);
  private static final Set<String> SUPPORTED_TYPES = Set.of("Colors", "CupertinoColors", "Icons", "CupertinoIcons");

  // Consulo: CompletionContributor is a language extension (upstream: language="Dart" in plugin.xml)
  @NotNull
  @Override
  public Language getLanguage() {
    return DartLanguage.INSTANCE;
  }

  @Override
  public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
    // Inspect the qualified reference directly enclosing the caret (not the topmost chain) so
    // mid-chain completions like `Colors.<caret>.withValues(...)` work, and take the last segment
    // of its qualifier so import prefixes like `m.Colors.<caret>` are handled.
    final PsiElement positionParent = parameters.getPosition().getParent();
    final PsiElement leaf = positionParent != null ? positionParent.getParent() : null;
    if (!(leaf instanceof DartReferenceExpression leafRef)) return;
    if (!(leafRef.getParent() instanceof DartReferenceExpression qualifiedRef)) return;
    if (qualifiedRef.getLastChild() != leafRef) return;

    if (!(qualifiedRef.getFirstChild() instanceof DartReferenceExpression qualifier)) return;
    final PsiElement qualifierLast = qualifier.getLastChild();
    final String prefix = qualifierLast != null ? qualifierLast.getText() : null;
    if (prefix == null || !SUPPORTED_TYPES.contains(prefix)) return;

    result.runRemainingContributors(parameters, otherResult -> {
      final String lookupString = otherResult.getLookupElement().getLookupString();
      final Image icon = findIcon(prefix, lookupString);
      if (icon != null) {
        final FlutterIconLookupElementDecorator decorated = new FlutterIconLookupElementDecorator(otherResult.getLookupElement(), icon);
        result.passResult(otherResult.withLookupElement(decorated));
      }
      else {
        result.passResult(otherResult);
      }
    });
  }

  private static class FlutterIconLookupElementDecorator extends LookupElementDecorator<LookupElement> {
    private final Image icon;

    FlutterIconLookupElementDecorator(@NotNull LookupElement delegate, @NotNull Image icon) {
      super(delegate);
      this.icon = icon;
    }

    @Override
    public void renderElement(LookupElementPresentation presentation) {
      super.renderElement(presentation);
      applyIcon(presentation);
    }

    @Nullable
    @Override
    public LookupElementRenderer<? extends LookupElement> getExpensiveRenderer() {
      final LookupElementRenderer<? extends LookupElement> delegateRenderer = getDelegate().getExpensiveRenderer();
      if (delegateRenderer == null) return null;
      return new LookupElementRenderer<FlutterIconLookupElementDecorator>() {
        @Override
        @SuppressWarnings("unchecked")
        public void renderElement(FlutterIconLookupElementDecorator element, LookupElementPresentation presentation) {
          ((LookupElementRenderer<LookupElement>)delegateRenderer).renderElement(element.getDelegate(), presentation);
          element.applyIcon(presentation);
        }
      };
    }

    private void applyIcon(@NotNull LookupElementPresentation presentation) {
      presentation.setTypeText("", icon);
      presentation.setTypeIconRightAligned(true);
    }
  }

  @Nullable
  public static Image findIcon(@Nullable String declaringType, @Nullable String name) {
    if (name == null) return null;
    if (declaringType == null) return null;
    switch (declaringType) {
      case "Colors": {
        final FlutterColors.FlutterColor color = FlutterColors.getColor(name);
        return color == null ? null : ImageEffects.colorFilled(ICON_SIZE, ICON_SIZE, TargetAWT.from(color.getAWTColor()));
      }
      case "CupertinoColors": {
        final FlutterColors.FlutterColor color = FlutterCupertinoColors.getColor(name);
        return color == null ? null : ImageEffects.colorFilled(ICON_SIZE, ICON_SIZE, TargetAWT.from(color.getAWTColor()));
      }
      case "Icons": {
        final Image icon = FlutterMaterialIcons.getIconForName(name);
        return icon != null ? icon : EMPTY_ICON;
      }
      case "CupertinoIcons": {
        final Image icon = FlutterCupertinoIcons.getIconForName(name);
        return icon != null ? icon : EMPTY_ICON;
      }
      default:
        return null;
    }
  }
}
