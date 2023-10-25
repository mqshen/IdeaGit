/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.execution.configurations;

import org.jetbrains.annotations.NotNull;

/**
 * Allow to get before/after steps from wrapped configuration: e.g. to rerun tests with initial tests before options
 */
public interface WrappingRunConfiguration<T extends RunConfiguration> extends WithoutOwnBeforeRunSteps {
  @NotNull T getPeer();

  static @NotNull RunProfile unwrapRunProfile(@NotNull RunProfile runProfile) {
    if (runProfile instanceof WrappingRunConfiguration) {
      return ((WrappingRunConfiguration<?>)runProfile).getPeer();
    }
    return runProfile;
  }
}
