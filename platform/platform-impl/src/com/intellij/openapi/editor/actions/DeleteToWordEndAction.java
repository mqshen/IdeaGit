// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package com.intellij.openapi.editor.actions;

public final class DeleteToWordEndAction extends TextComponentEditorAction {
  public DeleteToWordEndAction() {
    super(new DeleteToWordBoundaryHandler(false, false));
  }
}
