// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.serviceContainer

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.ide.plugins.cl.PluginAwareClassLoader
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.platform.instanceContainer.instantiation.instantiate
import com.intellij.platform.instanceContainer.internal.DynamicInstanceSupport
import com.intellij.platform.instanceContainer.internal.InstanceInitializer
import kotlinx.coroutines.CoroutineScope
import java.lang.invoke.MethodType

internal class LightServiceInstanceSupport(
  private val componentManager: ComponentManagerImpl,
  private val supportedSignatures: List<MethodType>,
) : DynamicInstanceSupport {

  override fun dynamicInstanceInitializer(instanceClass: Class<*>): InstanceInitializer? {
    if (!isLightService(instanceClass)) {
      return null
    }
    return LightServiceInstanceInitializer(instanceClass)
  }

  private inner class LightServiceInstanceInitializer(
    private val instanceClass: Class<*>,
  ) : InstanceInitializer {

    override val instanceClassName: String get() = instanceClass.name

    override fun loadInstanceClass(keyClass: Class<*>?): Class<*> = instanceClass

    override suspend fun createInstance(parentScope: CoroutineScope, instanceClass: Class<*>): Any {
      check(instanceClass === this.instanceClass)
      val instance = instantiate(
        componentManager.dependencyResolver,
        parentScope,
        instanceClass,
        supportedSignatures,
      )
      if (instance is Disposable) {
        Disposer.register(componentManager.serviceParentDisposable, instance)
      }
      val classLoader = instanceClass.classLoader
      val pluginId = if (classLoader is PluginAwareClassLoader) {
        classLoader.pluginId
      }
      else {
        PluginManagerCore.CORE_ID
      }
      componentManager.initializeComponent(instance, serviceDescriptor = null, pluginId)
      return instance
    }
  }
}
