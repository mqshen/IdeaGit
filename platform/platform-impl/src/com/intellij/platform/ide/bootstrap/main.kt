package com.intellij.platform.ide.bootstrap

import com.intellij.ide.bootstrap.InitAppContext
import com.intellij.idea.AppExitCodes
import com.intellij.idea.AppMode
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.application.impl.ApplicationInfoImpl
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.ShutDownTracker
import com.intellij.platform.diagnostic.telemetry.impl.span
import java.nio.file.Path
import kotlinx.coroutines.*
import kotlin.system.exitProcess


fun CoroutineScope.startApplication(args: List<String>,
                                    mainClassLoaderDeferred: Deferred<ClassLoader>?,
                                    appStarterDeferred: Deferred<AppStarter>,
                                    mainScope: CoroutineScope,
                                    busyThread: Thread) {
    val appInfoDeferred = async {
        mainClassLoaderDeferred?.await()
        span("app info") {
            // required for DisabledPluginsState and EUA
            ApplicationInfoImpl.getShadowInstance()
        }
    }

    val isHeadless = AppMode.isHeadless()

    val lockSystemDirsJob = launch {
        // the "import-needed" check must be performed strictly before IDE directories are locked
        span("system dirs locking") {
            lockSystemDirs(args)
        }
    }
}

private suspend fun lockSystemDirs(args: List<String>) {
    val directoryLock = DirectoryLock(PathManager.getConfigDir(), PathManager.getSystemDir()) { processorArgs ->
        @Suppress("RAW_RUN_BLOCKING")
        runBlocking {
            commandProcessor.get()(processorArgs).await()
        }
    }

    try {
        val currentDir = Path.of(System.getenv(LAUNCHER_INITIAL_DIRECTORY_ENV_VAR) ?: "").toAbsolutePath().normalize()
        val result = withContext(Dispatchers.IO) { directoryLock.lockOrActivate(currentDir, args) }
        if (result == null) {
            ShutDownTracker.getInstance().registerShutdownTask {
                try {
                    directoryLock.dispose()
                }
                catch (e: Throwable) {
                    logger<DirectoryLock>().error(e)
                }
            }
        }
        else {
            result.message?.let { println(it) }
            exitProcess(result.exitCode)
        }
    }
    catch (e: DirectoryLock.CannotActivateException) {
        if (args.isEmpty()) {
            StartupErrorReporter.showMessage(BootstrapBundle.message("bootstrap.error.title.start.failed"), e.message, true)
        }
        else {
            println(e.message)
        }
        exitProcess(AppExitCodes.INSTANCE_CHECK_FAILED)
    }
    catch (e: Throwable) {
        StartupErrorReporter.showMessage(BootstrapBundle.message("bootstrap.error.title.start.failed"), e)
        exitProcess(AppExitCodes.STARTUP_EXCEPTION)
    }
}

interface AppStarter {
    fun prepareStart(args: List<String>) {}

    suspend fun start(context: InitAppContext)

    /* called from IDE init thread */
    fun beforeImportConfigs() {}

    /* called from IDE init thread */
    fun importFinished(newConfigDir: Path) {}
}