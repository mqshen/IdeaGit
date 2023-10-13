// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.util.io.blobstorage;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;

@ApiStatus.Internal
public interface ByteBufferReader<T> {
  T read(final @NotNull ByteBuffer data) throws IOException;
}
