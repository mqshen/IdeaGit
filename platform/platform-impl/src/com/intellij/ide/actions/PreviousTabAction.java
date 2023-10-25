// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.ide.actions;

import com.intellij.ide.lightEdit.LightEditCompatible;
import com.intellij.openapi.actionSystem.remoting.ActionRemoteBehavior;
import org.jetbrains.annotations.NotNull;

public final class PreviousTabAction extends TabNavigationActionBase implements LightEditCompatible {
  public PreviousTabAction () { super (NavigationType.PREV); }

  @NotNull
  @Override
  public ActionRemoteBehavior getBehavior() {
    return ActionRemoteBehavior.FrontendOnly;
  }
}
