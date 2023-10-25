// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.ui;

import com.intellij.ide.ui.search.OptionDescription;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.function.Consumer;

public interface OptionsSearchTopHitProvider {
  @NotNull
  @NonNls
  String getId();

  default boolean preloadNeeded() {
    return true;
  }

  /**
   * Extension point name: com.intellij.search.topHitProvider
   */
  interface ApplicationLevelProvider extends OptionsSearchTopHitProvider{
    @NotNull Collection<OptionDescription> getOptions();

    // do not override

  }

  /**
   * Extension point name: com.intellij.search.projectOptionsTopHitProvider
   */
  interface ProjectLevelProvider extends OptionsSearchTopHitProvider {
    @NotNull
    Collection<OptionDescription> getOptions(@NotNull Project project);
  }
}
