@file:JvmName("Main")
@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package com.intellij.idea

import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.application.impl.ApplicationInfoImpl
import java.util.*
import java.util.function.Consumer
import kotlin.system.exitProcess

fun main(rawArgs: Array<String>) {
    val startupTimings = ArrayList<Any>(12)
    val startTimeNano = System.nanoTime()
    val startTimeUnixNano = System.currentTimeMillis() * 1000000
    startupTimings.add("startup begin")
    startupTimings.add(startTimeNano)
    mainImpl(rawArgs = rawArgs, startupTimings = startupTimings, startTimeUnixNano = startTimeUnixNano)
}

internal fun mainImpl(rawArgs: Array<String>,
                      startupTimings: ArrayList<Any>,
                      startTimeUnixNano: Long,
                      changeClassPath: Consumer<ClassLoader>? = null) {
    val args = preprocessArgs(rawArgs)

}

private fun preprocessArgs(args: Array<String>): List<String> {
    if (args.isEmpty()) {
        return Collections.emptyList()
    }
    // a buggy DE may fail to strip an unused parameter from a .desktop file
    if (args.size == 1 && args[0] == "%f") {
        return Collections.emptyList()
    }

    @Suppress("SuspiciousPackagePrivateAccess")
    if (AppMode.HELP_OPTION in args) {
        println("""
        Some of the common commands and options (sorry, the full list is not yet supported):
          --help      prints a short list of commands and options
          --version   shows version information
          /project/dir
            opens a project from the given directory
          [/project/dir|--temp-project] [--wait] [--line <line>] [--column <column>] file
            opens the file, either in a context of the given project or as a temporary single-file project,
            optionally waiting until the editor tab is closed
          diff <left> <right>
            opens a diff window between <left> and <right> files/directories
          merge <local> <remote> [base] <merged>
            opens a merge window between <local> and <remote> files (with optional common <base>), saving the result to <merged>
        """.trimIndent())
        exitProcess(0)
    }

    @Suppress("SuspiciousPackagePrivateAccess")
    if (AppMode.VERSION_OPTION in args) {
        val appInfo = ApplicationInfoImpl.getShadowInstance()
        val edition = ApplicationNamesInfo.getInstance().editionName?.let { " (${it})" } ?: ""
        println("${appInfo.fullApplicationName}${edition}\nBuild #${appInfo.build.asString()}")
        exitProcess(0)
    }

    val (propertyArgs, otherArgs) = args.partition { it.startsWith("-D") && it.contains('=') }
    propertyArgs.forEach { arg ->
        val (option, value) = arg.removePrefix("-D").split('=', limit = 2)
        System.setProperty(option, value)
    }
    return otherArgs
}