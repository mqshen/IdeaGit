// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.util;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

/**
 * Identifies a line separator:
 * either Unix ({@code \n}), Windows ({@code \r\n}) or (possible not actual anymore) Classic Mac ({@code \r}).
 * <p>The intention is to use this class everywhere, where a line separator is needed instead of just Strings.</p>
 *
 * @author Kirill Likhodedov
 */
public enum LineSeparator {
  LF("\n"),
  CRLF("\r\n"),
  CR("\r");

  private final String mySeparatorString;
  private final byte[] myBytes;

  LineSeparator(@NotNull String separatorString) {
    mySeparatorString = separatorString;
    myBytes = separatorString.getBytes(StandardCharsets.UTF_8);
  }

  @Override
  public @NlsSafe String toString() {
    return super.toString();
  }

  public static @NotNull LineSeparator fromString(@NotNull String string) {
    for (LineSeparator separator : values()) {
      if (separator.getSeparatorString().equals(string)) {
        return separator;
      }
    }
    Logger.getInstance(LineSeparator.class).error("Invalid string for line separator: " + StringUtil.escapeStringCharacters(string));
    return getSystemLineSeparator();
  }

  public @NotNull String getSeparatorString() {
    return mySeparatorString;
  }

  public byte @NotNull [] getSeparatorBytes() {
    return myBytes;
  }

  public static @NotNull LineSeparator getSystemLineSeparator() {
    return SystemInfo.isWindows ? CRLF : LF;
  }
}