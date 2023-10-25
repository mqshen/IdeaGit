// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.openapi.editor.actions;

import com.intellij.openapi.actionSystem.remoting.ActionRemoteBehavior;
import com.intellij.openapi.editor.Editor;
import org.jetbrains.annotations.NotNull;

public final class ToggleShowLineNumbersAction extends EditorToggleDecorationAction {
  @Override
  protected void setOption(Editor editor, boolean state) {
    editor.getSettings().setLineNumbersShown(state);
  }

  @Override
  protected boolean getOption(Editor editor) {
    return editor.getSettings().isLineNumbersShown();
  }

  @NotNull
  @Override
  public ActionRemoteBehavior getBehavior() {
    return ActionRemoteBehavior.FrontendOnly;
  }
}
