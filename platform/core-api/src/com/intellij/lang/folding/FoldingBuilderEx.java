// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package com.intellij.lang.folding;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Allows a custom language plugin to define rules for folding code in the language handled
 * by the plugin.
 *
 * @author max
 * @see LanguageFolding#forLanguage(com.intellij.lang.Language)
 */

public abstract class FoldingBuilderEx implements FoldingBuilder {
  /**
   * Builds the folding regions for the specified node in the AST tree and its children.
   *
   * @param root     the element for which folding is requested.
   * @param document the document for which folding is built. Can be used to retrieve line
   *                 numbers for folding regions.
   * @param quick    whether the result should be provided as soon as possible. Is true, when
   *                 an editor is opened and we need to auto-fold something immediately, like Java imports.
   *                 If true, one should perform no reference resolving and avoid complex checks if possible.
   * @return the array of folding descriptors.
   */
  public abstract FoldingDescriptor @NotNull [] buildFoldRegions(@NotNull PsiElement root, @NotNull Document document, boolean quick);

  @Override
  public FoldingDescriptor @NotNull [] buildFoldRegions(@NotNull ASTNode node, @NotNull Document document) {
    return buildFoldRegions(node.getPsi(), document, false);
  }

  /**
   * Returns the text which is displayed in the editor for the folding region related to the
   * specified node when the folding region is collapsed.
   *
   *
   * @param node the node for which the placeholder text is requested.
   * @param range text range within whole file to fold
   * @return the placeholder text.
   */
  public @Nullable String getPlaceholderText(@NotNull ASTNode node, @NotNull TextRange range){
    return getPlaceholderText(node);
  }

  /**
   * Returns the default collapsed state for the folding region related to the specified node.
   *
   * @param node the node for which the collapsed state is requested.
   * @return true if the region is collapsed by default, false otherwise.
   */
  @Override
  public abstract boolean isCollapsedByDefault(@NotNull ASTNode node);
}