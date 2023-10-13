// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.util

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import kotlinx.coroutines.*
import org.jetbrains.annotations.ApiStatus.Experimental
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Awaits cancellation of [this] scope, and executes [action] in the context dispatcher of the scope after the scope is cancelled.
 * NB: this function prevents normal completion of the scope, so it should be used for scopes which don't complete normally anyway.
 *
 * Standard [Job.invokeOnCompletion] does not provide any threading guarantees,
 * but there are cases when the cleanup action is expected to be invoked on a certain thread (e.g. EDT).
 * See https://github.com/Kotlin/kotlinx.coroutines/issues/3505
 *
 * @param ctx additional context for the cleaner-coroutine, e.g. [CoroutineName]
 */
@Experimental
fun CoroutineScope.awaitCancellationAndInvoke(ctx: CoroutineContext = EmptyCoroutineContext, action: suspend CoroutineScope.() -> Unit) {
  requireNoJob(ctx)
  // UNDISPATCHED guarantees that the coroutine will execute until the first suspension point (awaitCancellation)
  launch(ctx, start = CoroutineStart.UNDISPATCHED) {
    try {
      awaitCancellation()
    }
    finally {
      withContext(NonCancellable + ModalityState.any().asContextElement()) {
        // yield forces re-dispatch guaranteeing that the action won't be executed right away
        // in case the current scope was cancelled concurrently
        yield()
        action()
      }
    }
  }
}
