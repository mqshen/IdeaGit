// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.io;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface SelfDiagnosing {
  void diagnose();
}
