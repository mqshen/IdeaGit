// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.psi.scope;

import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Base processor which stores hints in a map
 */
public abstract class ProcessorWithHints implements PsiScopeProcessor {
  private final Map<Key<?>, Object> myHints = new HashMap<>();

  protected final <H> void hint(@NotNull Key<H> key, @NotNull H hint) {
    myHints.put(key, hint);
  }

  @Override
  public @Nullable <T> T getHint(@NotNull Key<T> hintKey) {
    return (T)myHints.get(hintKey);
  }
}
