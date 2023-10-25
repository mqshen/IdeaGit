// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.openapi.editor.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.remoting.ActionRemoteBehavior;
import com.intellij.openapi.actionSystem.remoting.ActionRemoteBehaviorSpecification;
import org.jetbrains.annotations.NotNull;

public final class CreateRectangularSelectionOnMouseDragAction extends AnAction implements ActionRemoteBehaviorSpecification.Frontend {
  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    // actual logic is implemented in EditorImpl
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabled(false);
  }

  @NotNull
  @Override
  public ActionRemoteBehavior getBehavior() {
    return ActionRemoteBehavior.FrontendOnly;
  }
}
