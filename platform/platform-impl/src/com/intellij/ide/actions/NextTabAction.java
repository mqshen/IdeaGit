// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions;

import com.intellij.ide.lightEdit.LightEditCompatible;
import com.intellij.openapi.actionSystem.remoting.ActionRemoteBehavior;
import org.jetbrains.annotations.NotNull;

public final class NextTabAction extends TabNavigationActionBase implements LightEditCompatible {
  public NextTabAction () { super (NavigationType.NEXT); }

  @NotNull
  @Override
  public ActionRemoteBehavior getBehavior() {
    return ActionRemoteBehavior.FrontendOnly;
  }
}
