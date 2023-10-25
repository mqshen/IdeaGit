// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.openapi.project.impl

import com.intellij.configurationStore.runInAutoSaveDisabledMode
import com.intellij.configurationStore.saveSettings
import com.intellij.ide.SaveAndSyncHandler
import com.intellij.ide.impl.runUnderModalProgressIfIsEdt
import com.intellij.ide.plugins.ContainerDescriptor
import com.intellij.ide.plugins.IdeaPluginDescriptorImpl
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.ide.startup.StartupManagerEx
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.openapi.application.impl.LaterInvocator
import com.intellij.openapi.client.ClientAwareComponentManager
import com.intellij.openapi.components.StorageScheme
import com.intellij.openapi.components.impl.stores.IProjectStore
import com.intellij.openapi.components.service
import com.intellij.openapi.components.serviceIfCreated
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectNameListener
import com.intellij.openapi.project.ex.ProjectEx
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.impl.FrameTitleBuilder
import com.intellij.project.ProjectStoreOwner
import com.intellij.serviceContainer.*
import com.intellij.util.ExceptionUtil
import com.intellij.util.TimedReference
import com.intellij.util.childScope
import com.intellij.util.concurrency.SynchronizedClearableLazy
import com.intellij.util.io.systemIndependentPath
import com.intellij.util.messages.impl.MessageBusEx
import kotlinx.coroutines.*
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.ApiStatus.Internal
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.TestOnly
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.nio.file.ClosedFileSystemException
import java.nio.file.Path
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicReference

internal val projectMethodType: MethodType = MethodType.methodType(Void.TYPE, Project::class.java)
internal val projectAndScopeMethodType: MethodType = MethodType.methodType(Void.TYPE, Project::class.java, CoroutineScope::class.java)

private val LOG = logger<ProjectImpl>()

@Internal
open class ProjectImpl(parent: ComponentManagerImpl, filePath: Path, projectName: String?)
  : ClientAwareComponentManager(parent), ProjectEx, ProjectStoreOwner {
  companion object {
    @Internal
    val RUN_START_UP_ACTIVITIES: Key<Boolean> = Key.create("RUN_START_UP_ACTIVITIES")

    @JvmField
    val CREATION_TIME: Key<Long> = Key.create("ProjectImpl.CREATION_TIME")

    @JvmField
    val PROJECT_PATH = Key.create<Path>("ProjectImpl.PROJECT_PATH")

    @TestOnly
    const val LIGHT_PROJECT_NAME: @NonNls String = "light_temp"

    private val CREATION_TRACE = Key.create<String>("ProjectImpl.CREATION_TRACE")

    @TestOnly
    @JvmField
    val CREATION_TEST_NAME: Key<String> = Key.create("ProjectImpl.CREATION_TEST_NAME")

    @TestOnly
    @JvmField
    val USED_TEST_NAMES: Key<String> = Key.create("ProjectImpl.USED_TEST_NAMES")

    // for light projects, preload only services that are essential
    // ("await" means "project component loading activity is completed only when all such services are completed")
    internal fun CoroutineScope.schedulePreloadServices(project: ProjectImpl) {
      launch(CoroutineName("project service preloading (sync)")) {
        project.preloadServices(modules = PluginManagerCore.getPluginSet().getEnabledModules(),
                                activityPrefix = "project ",
                                syncScope = this,
                                onlyIfAwait = project.isLight,
                                asyncScope = project.asyncPreloadServiceScope)
      }
    }
  }

  // used by Rider
  @Internal
  @JvmField
  val asyncPreloadServiceScope: CoroutineScope = coroutineScope.childScope(supervisor = false)

  private val earlyDisposable = AtomicReference(Disposer.newDisposable())

  @Volatile
  var isTemporarilyDisposed: Boolean = false
    private set

  private val isLight: Boolean

  private var cachedName: String?

  private val componentStoreValue = SynchronizedClearableLazy {
    ApplicationManager.getApplication().service<ProjectStoreFactory>().createStore(this)
  }

  init {
    storeCreationTrace()

    @Suppress("LeakingThis")
    putUserData(CREATION_TIME, System.nanoTime())

    @Suppress("LeakingThis")
    registerServiceInstance(Project::class.java, this, fakeCorePluginDescriptor)

    cachedName = projectName
    // light project may be changed later during test, so we need to remember its initial state
    @Suppress("TestOnlyProblems")
    isLight = ApplicationManager.getApplication().isUnitTestMode && filePath.toString().contains(LIGHT_PROJECT_NAME)
  }

  final override fun <T : Any> findConstructorAndInstantiateClass(lookup: MethodHandles.Lookup, aClass: Class<T>): T {
    @Suppress("UNCHECKED_CAST")
    // see ConfigurableEP - prefer constructor that accepts our instance
    return (lookup.findConstructorOrNull(aClass, projectMethodType)?.invoke(this)
            ?: lookup.findConstructorOrNull(aClass, projectAndScopeMethodType)?.invoke(this, instanceCoroutineScope(aClass))
            ?: lookup.findConstructorOrNull(aClass, coroutineScopeMethodType)?.invoke(instanceCoroutineScope(aClass))
            ?: lookup.findConstructorOrNull(aClass, emptyConstructorMethodType)?.invoke()
            ?: throw RuntimeException("Cannot find suitable constructor, " +
                                      "expected (Project), (Project, CoroutineScope), (CoroutineScope), or ()")) as T
  }

  final override fun supportedSignaturesOfLightServiceConstructors(): List<MethodType> {
    return listOf(
      projectMethodType,
      projectAndScopeMethodType,
      coroutineScopeMethodType,
      emptyConstructorMethodType,
    )
  }

  override fun isInitialized(): Boolean {
    val containerState = containerState.get()
    if ((containerState < ContainerState.COMPONENT_CREATED || containerState >= ContainerState.DISPOSE_IN_PROGRESS)
        || isTemporarilyDisposed
        || !isOpen) {
      return false
    }
    else if (ApplicationManager.getApplication().isUnitTestMode && getUserData(RUN_START_UP_ACTIVITIES) == false) {
      // if test asks to not run RUN_START_UP_ACTIVITIES, it means "ignore start-up activities", but project considered as initialized
      return true
    }
    else {
      return (serviceIfCreated<StartupManager>() as StartupManagerEx?)?.startupActivityPassed() == true
    }
  }

  override fun getName(): String {
    var result = cachedName
    if (result == null) {
      // ProjectPathMacroManager adds macro PROJECT_NAME_MACRO_NAME and so, project name is required on each load of configuration file.
      // So the name is computed very early anyway.
      result = componentStore.projectName
      cachedName = result
    }
    return result
  }

  override fun setProjectName(value: String) {
    if (cachedName == value) {
      return
    }

    cachedName = value

    if (!ApplicationManager.getApplication().isUnitTestMode) {
      messageBus.syncPublisher(ProjectNameListener.TOPIC).nameChanged(value)
      StartupManager.getInstance(this).runAfterOpened {
        ApplicationManager.getApplication().invokeLater(Runnable {
          val frame = WindowManager.getInstance().getFrame(this) ?: return@Runnable
          val title = FrameTitleBuilder.getInstance().getProjectTitle(this) ?: return@Runnable
          frame.title = title
        }, ModalityState.nonModal(), disposed)
      }
    }
  }

  final override val componentStore: IProjectStore
    get() = componentStoreValue.value

  final override fun getProjectFilePath(): String = componentStore.projectFilePath.systemIndependentPath

  final override fun getProjectFile(): VirtualFile? {
    return LocalFileSystem.getInstance().findFileByNioFile(componentStore.projectFilePath)
  }

  @Suppress("DeprecatedCallableAddReplaceWith")
  @Deprecated("Deprecated in Java")
  final override fun getBaseDir(): VirtualFile? {
    return LocalFileSystem.getInstance().findFileByNioFile(componentStore.projectBasePath)
  }

  final override fun getBasePath(): String = componentStore.projectBasePath.systemIndependentPath

  final override fun getPresentableUrl(): String = componentStore.presentableUrl

  override fun getLocationHash(): String {
    val store = componentStore
    val prefix: String
    val path: Path
    if (store.storageScheme == StorageScheme.DIRECTORY_BASED) {
      path = store.projectBasePath
      prefix = ""
    }
    else {
      path = store.projectFilePath
      prefix = getName()
    }
    return "$prefix${Integer.toHexString(path.systemIndependentPath.hashCode())}"
  }

  final override fun getWorkspaceFile(): VirtualFile? {
    return LocalFileSystem.getInstance().findFileByNioFile(componentStore.workspacePath)
  }

  final override fun isLight(): Boolean = isLight

  @Internal
  final override fun activityNamePrefix(): String = "project "

  @TestOnly
  fun setTemporarilyDisposed(value: Boolean) {
    if (isTemporarilyDisposed == value) {
      return
    }

    if (value && super.isDisposed()) {
      throw IllegalStateException("Project was already disposed, flag temporarilyDisposed cannot be set to `true`")
    }

    if (!value) {
      val newDisposable = Disposer.newDisposable()
      if (!earlyDisposable.compareAndSet(null, newDisposable)) {
        throw IllegalStateException("earlyDisposable must be null on second opening of light project")
      }
    }

    // Must be not only on temporarilyDisposed = true, but also on temporarilyDisposed = false,
    // because events are fired for temporarilyDisposed project between project closing and project opening,
    // and it can lead to cache population.
    // Message bus implementation can be complicated to add "owner.isDisposed" check before getting subscribers,
    // but as the bus is a very important subsystem, it's better to not add any non-production logic.

    // light project is not disposed, so, subscriber cache contains handlers that will handle events for a temporarily disposed project,
    // so, we clear subscriber cache. `isDisposed` for project returns `true` if `temporarilyDisposed`, so, handler will be not added.
    (messageBus as MessageBusEx).clearAllSubscriberCache()
    isTemporarilyDisposed = value
  }

  @ApiStatus.Experimental
  @Internal
  final override fun getEarlyDisposable(): Disposable {
    if (isDisposed) {
      throw AlreadyDisposedException("$this is disposed already")
    }
    return earlyDisposable.get() ?: throw createEarlyDisposableError("earlyDisposable is null for")
  }

  private val DISPOSE_EARLY_DISPOSABLE_TRACE = Key.create<String>("ProjectImpl.DISPOSE_EARLY_DISPOSABLE_TRACE")

  fun disposeEarlyDisposable() {
    if (LOG.isDebugEnabled || ApplicationManager.getApplication().isUnitTestMode) {
      LOG.debug("dispose early disposable: ${toString()}")
    }

    val disposable = earlyDisposable.getAndSet(null) ?: throw createEarlyDisposableError("earlyDisposable was already disposed")
    if (ApplicationManager.getApplication().isUnitTestMode) {
      putUserData(DISPOSE_EARLY_DISPOSABLE_TRACE, ExceptionUtil.currentStackTrace())
    }
    Disposer.dispose(disposable)
  }

  private fun createEarlyDisposableError(error: String): RuntimeException {
    return IllegalStateException("$error for ${toString()}\n---begin of dispose trace--" +
                                 getUserData(DISPOSE_EARLY_DISPOSABLE_TRACE) +
                                 "}---end of dispose trace---\n")
  }

  final override fun isDisposed(): Boolean = super.isDisposed() || isTemporarilyDisposed

  @Synchronized
  final override fun dispose() {
    val app = ApplicationManager.getApplication()
    // dispose must be under a write action
    app.assertWriteAccessAllowed()
    val projectManager = ProjectManager.getInstance() as ProjectManagerImpl

    // can call dispose only via com.intellij.ide.impl.ProjectUtil.closeAndDispose()
    if (projectManager.isProjectOpened(this)) {
      throw IllegalStateException("Must call .dispose() for a closed project only. " +
                                  "See ProjectManager.closeProject() or ProjectUtil.closeAndDispose().")
    }

    super.dispose()

    componentStoreValue.valueIfInitialized?.release()

    if (!app.isDisposed) {
      @Suppress("DEPRECATION", "removal")
      app.messageBus.syncPublisher(ProjectLifecycleListener.TOPIC).afterProjectClosed(this)
    }
    projectManager.updateTheOnlyProjectField()
    TimedReference.disposeTimed()
    LaterInvocator.purgeExpiredItems()
  }

  @TestOnly
  fun setLightProjectName(name: String) {
    assert(isLight)
    setProjectName(name)
    storeCreationTrace()
  }

  final override fun toString(): String {
    val store = componentStoreValue.valueIfInitialized
    val containerState = if (isTemporarilyDisposed) "disposed temporarily" else containerStateName
    val componentStore = if (store == null) {
      "<not initialized>"
    }
    else {
      try {
        if (store.storageScheme == StorageScheme.DIRECTORY_BASED) {
          store.projectBasePath.toString()
        }
        else {
          store.projectFilePath
        }
      }
      catch (e: ClosedFileSystemException) {
        "<fs closed>"
      }
    }
    val disposedStr = if (isDisposed) " (disposed)" else ""
    return "Project(name=$cachedName, containerState=$containerState, componentStore=$componentStore)$disposedStr"
  }

  override fun isOpen(): Boolean {
    val projectManager = ProjectManagerEx.getInstanceExIfCreated()
    return projectManager != null && projectManager.isProjectOpened(this)
  }

  override fun getContainerDescriptor(pluginDescriptor: IdeaPluginDescriptorImpl): ContainerDescriptor = pluginDescriptor.projectContainerDescriptor

  override fun scheduleSave() {
    SaveAndSyncHandler.getInstance().scheduleSave(SaveAndSyncHandler.SaveTask(project = this))
  }

  override fun save() {
    val app = ApplicationManagerEx.getApplicationEx()
    if (!app.isSaveAllowed) {
      // no need to save
      return
    }

    // ensure that expensive save operation is not performed before startupActivityPassed
    // first save may be quite cost operation, because cache is not warmed up yet
    if (!isInitialized) {
      LOG.debug { "Skip save for $name: not initialized" }
      return
    }

    runInAutoSaveDisabledMode {
      runUnderModalProgressIfIsEdt {
        saveSettings(componentManager = this@ProjectImpl)
      }
    }
  }

  @TestOnly
  final override fun getCreationTrace(): String? {
    val trace = getUserData(CREATION_TRACE)
    val testName = getUserData(CREATION_TEST_NAME) ?: return trace
    return "created in test: $testName, used in tests: ${getUserData(USED_TEST_NAMES)}\n $trace"
  }

  private fun storeCreationTrace() {
    if (ApplicationManager.getApplication().isUnitTestMode) {
      putUserData(CREATION_TRACE, "${LocalDateTime.now()}@${ExceptionUtil.currentStackTrace()}")
    }
  }

  final override fun stopServicePreloading() {
    super.stopServicePreloading()

    asyncPreloadServiceScope.cancel()
  }
}