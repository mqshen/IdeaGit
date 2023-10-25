// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.openapi.editor.actions;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.util.registry.Registry;
import org.jetbrains.annotations.NotNull;

final class MoveCaretUpOrDownHandler extends EditorActionHandler.ForEachCaret {
  enum Direction {UP, DOWN}

  private final @NotNull Direction myDirection;

  MoveCaretUpOrDownHandler(@NotNull Direction direction) {
    myDirection = direction;
  }

  @Override
  public void doExecute(@NotNull Editor editor, @NotNull Caret caret, DataContext dataContext) {
    Runnable runnable = () -> {
      if (caret.hasSelection() && (!(editor instanceof EditorEx) || !((EditorEx)editor).isStickySelection()) &&
          !Registry.is("editor.action.caretMovement.UpDownIgnoreSelectionBoundaries", false)) {
        int targetOffset = myDirection == Direction.DOWN ? caret.getSelectionEnd()
                                                         : caret.getSelectionStart();
        caret.moveToOffset(targetOffset);
      }

      int lineShift = myDirection == Direction.DOWN ? 1 : -1;
      caret.moveCaretRelatively(0, lineShift, false,
                                caret == editor.getCaretModel().getPrimaryCaret());
    };
    EditorUtil.runWithAnimationDisabled(editor, runnable);
  }

  @Override
  public boolean isEnabledForCaret(@NotNull Editor editor, @NotNull Caret caret, DataContext dataContext) {
    return !editor.isOneLineMode();
  }
}
