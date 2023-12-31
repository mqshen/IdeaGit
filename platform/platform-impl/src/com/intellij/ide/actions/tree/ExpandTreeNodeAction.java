// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.tree;

import com.intellij.openapi.actionSystem.remoting.ActionRemoteBehavior;
import com.intellij.ui.TreeExpandCollapse;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

final class ExpandTreeNodeAction extends BaseTreeNodeAction {
  @Override
  protected void performOn(JTree tree) {
    TreeExpandCollapse.expand(tree);
  }
  @NotNull
  @Override
  public ActionRemoteBehavior getBehavior() {
    return ActionRemoteBehavior.FrontendOnly;
  }
}
