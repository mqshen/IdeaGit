// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.openapi.editor.actions;

import com.intellij.openapi.actionSystem.remoting.ActionRemoteBehavior;
import com.intellij.openapi.actionSystem.remoting.ActionRemoteBehaviorSpecification;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Shows a tooltip over a gutter icon when it's selected as an accessible element.
 * The real handler is installed from the editor gutter component.
 *
 * @author tav
 */
public final class ShowGutterIconTooltipAction extends EditorAction implements ActionRemoteBehaviorSpecification.Frontend {
  public ShowGutterIconTooltipAction() {
    super(new EditorActionHandler() {});
  }

  @NotNull
  @Override
  public ActionRemoteBehavior getBehavior() {
    return ActionRemoteBehavior.FrontendOnly;
  }
}
