@file:JvmName("Main")
@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package com.intellij.idea

import com.intellij.concurrency.IdeaForkJoinWorkerThreadFactory
import com.intellij.diagnostic.CoroutineTracerShim
import com.intellij.diagnostic.StartUpMeasurer
import com.intellij.ide.BootstrapBundle
import com.intellij.ide.plugins.StartupAbortedException
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.application.impl.ApplicationInfoImpl
import com.intellij.platform.diagnostic.telemetry.impl.rootTask
import com.intellij.platform.diagnostic.telemetry.impl.span
import com.intellij.platform.ide.bootstrap.AppStarter
import com.intellij.platform.ide.bootstrap.startApplication
import kotlinx.coroutines.runBlocking
import java.util.*
import java.util.function.Consumer
import kotlin.system.exitProcess
import kotlinx.coroutines.*
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

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
    AppMode.setFlags(args)
    addBootstrapTiming("AppMode.setFlags", startupTimings)
    try {
        PathManager.loadProperties()
        addBootstrapTiming("properties loading", startupTimings)
        PathManager.customizePaths()
        addBootstrapTiming("customizePaths", startupTimings)

        @Suppress("RAW_RUN_BLOCKING")
        runBlocking {
            addBootstrapTiming("main scope creating", startupTimings)

            val busyThread = Thread.currentThread()
            withContext(Dispatchers.Default + StartupAbortedExceptionHandler() + rootTask()) {
                addBootstrapTiming("init scope creating", startupTimings)
                StartUpMeasurer.addTimings(startupTimings, "bootstrap", startTimeUnixNano)
                startApp(args = args, mainScope = this@runBlocking, busyThread = busyThread, changeClassPath = changeClassPath)
            }

            awaitCancellation()
        }
    } catch (e: Throwable) {
        StartupErrorReporter.showMessage(BootstrapBundle.message("bootstrap.error.title.start.failed"), e)
        exitProcess(AppExitCodes.STARTUP_EXCEPTION)
    }
}

private suspend fun startApp(args: List<String>, mainScope: CoroutineScope, busyThread: Thread, changeClassPath: Consumer<ClassLoader>?) {
    span("startApplication") {
        launch {
            CoroutineTracerShim.coroutineTracer = object : CoroutineTracerShim {
                override suspend fun getTraceActivity() = com.intellij.platform.diagnostic.telemetry.impl.getTraceActivity()
                override fun rootTrace() = rootTask()

                override suspend fun <T> span(name: String, context: CoroutineContext, action: suspend CoroutineScope.() -> T): T {
                    return com.intellij.platform.diagnostic.telemetry.impl.span(name = name, context = context, action = action)
                }
            }
        }

        launch(CoroutineName("ForkJoin CommonPool configuration")) {
            IdeaForkJoinWorkerThreadFactory.setupForkJoinCommonPool(AppMode.isHeadless())
        }

        // must be after initMarketplace because initMarketplace can affect the main class loading (byte code transformer)
        val appStarterDeferred: Deferred<AppStarter>
        val mainClassLoaderDeferred: Deferred<ClassLoader>?
        if (changeClassPath == null) {
            appStarterDeferred = async(CoroutineName("main class loading")) {
                val aClass = AppMode::class.java.classLoader.loadClass("com.intellij.idea.MainImpl")
                MethodHandles.lookup().findConstructor(aClass, MethodType.methodType(Void.TYPE)).invoke() as AppStarter
            }
            mainClassLoaderDeferred = null
        }
        else {
            mainClassLoaderDeferred = async(CoroutineName("main class loader initializing")) {
                val classLoader = AppMode::class.java.classLoader
                changeClassPath.accept(classLoader)
                classLoader
            }

            appStarterDeferred = async(CoroutineName("main class loading")) {
                val aClass = mainClassLoaderDeferred.await().loadClass("com.intellij.idea.MainImpl")
                MethodHandles.lookup().findConstructor(aClass, MethodType.methodType(Void.TYPE)).invoke() as AppStarter
            }
        }

        startApplication(args = args,
            mainClassLoaderDeferred = mainClassLoaderDeferred,
            appStarterDeferred = appStarterDeferred,
            mainScope = mainScope,
            busyThread = busyThread)
    }
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

private fun addBootstrapTiming(name: String, startupTimings: MutableList<Any>) {
    startupTimings.add(name)
    startupTimings.add(System.nanoTime())
}

// separate class for nicer presentation in dumps
private class StartupAbortedExceptionHandler : AbstractCoroutineContextElement(CoroutineExceptionHandler), CoroutineExceptionHandler {
    override fun handleException(context: CoroutineContext, exception: Throwable) {
        StartupAbortedException.processException(exception)
    }

    override fun toString() = "StartupAbortedExceptionHandler"
}