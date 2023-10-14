// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.process

import com.intellij.openapi.util.registry.Registry

class LocalPtyOptions private constructor(val consoleMode: Boolean,
                                          val useCygwinLaunch: Boolean,
                                          val initialColumns: Int,
                                          val initialRows: Int,
                                          val useWinConPty: Boolean) {

  override fun toString(): String {
    return "consoleMode=$consoleMode, useCygwinLaunch=$useCygwinLaunch, initialColumns=$initialColumns, initialRows=$initialRows, useWinConPty=$useWinConPty"
  }

  fun builder(): Builder {
    return Builder(consoleMode, useCygwinLaunch, initialColumns, initialRows, useWinConPty)
  }

  companion object {
    @JvmField
    val DEFAULT: LocalPtyOptions = LocalPtyOptions(false, false, -1, -1, false)

    @JvmStatic
    fun shouldUseWinConPty() : Boolean = Registry.`is`("terminal.use.conpty.on.windows", false)
  }

  class Builder internal constructor(private var consoleMode: Boolean,
                                     private var useCygwinLaunch: Boolean,
                                     private var initialColumns: Int,
                                     private var initialRows: Int,
                                     private var useWinConPty: Boolean) {

    /**
     * @param consoleMode `true` means that started process output will be shown using `ConsoleViewImpl`:
     * 1. TTY echo is disabled (like `stty -echo`) to use manual input text buffer provided by `ConsoleViewImpl`.
     * 2. Separate process `stderr` will be available. For example, `stderr` and `stdout` are merged in a terminal.
     *
     * `false` means that started process output will be shown using `TerminalExecutionConsole` that is based on a terminal emulator.
     */
    fun consoleMode(consoleMode: Boolean): Builder = apply { this.consoleMode = consoleMode }
    fun consoleMode(): Boolean = consoleMode
    fun useCygwinLaunch(useCygwinLaunch: Boolean): Builder = apply { this.useCygwinLaunch = useCygwinLaunch }
    fun useCygwinLaunch(): Boolean = useCygwinLaunch
    fun initialColumns(initialColumns: Int): Builder = apply { this.initialColumns = initialColumns }
    fun initialColumns(): Int = initialColumns
    fun initialRows(initialRows: Int): Builder = apply { this.initialRows = initialRows }
    fun initialRows(): Int = initialRows
    fun useWinConPty(useWinConPty: Boolean): Builder = apply { this.useWinConPty = useWinConPty }
    fun useWinConPty(): Boolean = useWinConPty

    fun build(): LocalPtyOptions = LocalPtyOptions(consoleMode, useCygwinLaunch, initialColumns, initialRows, useWinConPty)

    fun set(options: LocalPtyOptions): Builder = apply {
      consoleMode = options.consoleMode
      useCygwinLaunch = options.useCygwinLaunch
      initialColumns = options.initialColumns
      initialRows = options.initialRows
      useWinConPty = options.useWinConPty
    }

    override fun toString(): String {
      return build().toString()
    }
  }
}