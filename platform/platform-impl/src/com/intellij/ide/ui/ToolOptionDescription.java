// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.ui;

import com.intellij.ide.ui.search.BooleanOptionDescription;
import com.intellij.openapi.project.Project;

/**
 * @author Konstantin Bulenkov
 */
public final class ToolOptionDescription extends BooleanOptionDescription {


  public ToolOptionDescription(String option, String configurableId) {
    super(option, configurableId);
  }

  @Override
  public boolean isOptionEnabled() {
    return false;
  }

  @Override
  public void setOptionState(boolean enabled) {

  }
}
