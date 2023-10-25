// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.openapi.project

import com.intellij.util.messages.Topic
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Experimental
abstract class ProjectCloseListener {
  companion object {
    @Topic.AppLevel
    @JvmField
    val TOPIC: Topic<ProjectCloseListener> = Topic(ProjectCloseListener::class.java, Topic.BroadcastDirection.TO_DIRECT_CHILDREN, true)
  }

  /**
   * Invoked on project close. Works only if subscribed to an application message bus,
   * because, at this point, project-level bus connections are disconnected.
   */
  open fun projectClosed(project: Project) {}

  /**
   * Invoked on project close before any closing activities.
   */
  open fun projectClosing(project: Project) {}

  open fun projectClosingBeforeSave(project: Project) {}
}