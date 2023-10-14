// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.process;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.util.NlsContexts.DialogMessage;

public class ProcessNotCreatedException extends ExecutionException {
  private final GeneralCommandLine myCommandLine;

  public ProcessNotCreatedException(final @DialogMessage String message, final GeneralCommandLine commandLine) {
    super(message);
    myCommandLine = commandLine;
  }

  public ProcessNotCreatedException(final @DialogMessage String message, final Throwable cause, final GeneralCommandLine commandLine) {
    super(message, cause);
    myCommandLine = commandLine;
  }

  public GeneralCommandLine getCommandLine() {
    return myCommandLine;
  }
}
