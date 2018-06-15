/*
 * Copyright 2018, ConsenSys Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.consensys.cava.concurrent.coroutines.experimental

import kotlinx.coroutines.experimental.CancellableContinuation
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.CompletableDeferred
import kotlinx.coroutines.experimental.CoroutineDispatcher
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.CoroutineStart
import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.newCoroutineContext
import kotlinx.coroutines.experimental.suspendCancellableCoroutine
import net.consensys.cava.concurrent.AsyncCompletion
import net.consensys.cava.concurrent.CompletableAsyncCompletion
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletionException
import java.util.function.Consumer
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.ContinuationInterceptor
import kotlin.coroutines.experimental.CoroutineContext

/**
 * Starts new coroutine and returns its result as an [AsyncCompletion].
 *
 * This coroutine builder uses [CommonPool] context by default and is conceptually similar to
 * [AsyncCompletion.executeBlocking].
 *
 * The running coroutine is cancelled when the [AsyncCompletion] is cancelled or otherwise completed.
 *
 * The [context] for the new coroutine can be explicitly specified. See [CoroutineDispatcher] for the standard context
 * implementations that are provided by `kotlinx.coroutines`. The [context][CoroutineScope.coroutineContext] of the
 * parent coroutine from its [scope][CoroutineScope] may be used, in which case the [Job] of the resulting coroutine is
 * a child of the job of the parent coroutine. The parent job may be also explicitly specified using [parent]
 * parameter.
 *
 * If the context does not have any dispatcher nor any other [ContinuationInterceptor], then [DefaultDispatcher] is
 * used.
 *
 * By default, the coroutine is immediately scheduled for execution. Other options can be specified via `start`
 * parameter. See [CoroutineStart] for details. A value of [CoroutineStart.LAZY] is not supported (since
 * [AsyncCompletion] does not provide the corresponding capability) and produces [IllegalArgumentException].
 *
 * See [newCoroutineContext] for a description of debugging facilities that are available for newly created coroutine.
 *
 * @param context context of the coroutine. The default value is [DefaultDispatcher].
 * @param start coroutine start option. The default value is [CoroutineStart.DEFAULT].
 * @param parent explicitly specifies the parent job, overrides job from the [context] (if any).
 * @param block the coroutine code.
 */
fun asyncCompletion(
  context: CoroutineContext = DefaultDispatcher,
  start: CoroutineStart = CoroutineStart.DEFAULT,
  parent: Job? = null,
  block: suspend CoroutineScope.() -> Unit
): AsyncCompletion {
  require(!start.isLazy) { "$start start is not supported" }
  val newContext = newCoroutineContext(context, parent)
  val job = Job(newContext[Job])
  val coroutine = AsyncCompletionCoroutine(newContext + job)
  job.invokeOnCompletion { coroutine.completion.cancel() }
  coroutine.completion.whenComplete { exception -> job.cancel(exception) }
  start(block, receiver = coroutine, completion = coroutine) // use the specified start strategy
  return coroutine.completion
}

private class AsyncCompletionCoroutine(
  override val context: CoroutineContext,
  val completion: CompletableAsyncCompletion = AsyncCompletion.incomplete()
) : Continuation<Unit>, CoroutineScope {
  override val coroutineContext: CoroutineContext get() = context
  override val isActive: Boolean get() = context[Job]!!.isActive
  override fun resume(value: Unit) {
    completion.complete()
  }

  override fun resumeWithException(exception: Throwable) {
    completion.completeExceptionally(exception)
  }
}

/**
 * Converts this deferred value to a [AsyncCompletion].
 * The deferred value is cancelled when the returned [AsyncCompletion] is cancelled or otherwise completed.
 */
fun Deferred<Unit>.asAsyncCompletion(): AsyncCompletion {
  val asyncCompletion = AsyncCompletion.incomplete()
  asyncCompletion.whenComplete { exception -> cancel(exception) }
  invokeOnCompletion {
    try {
      asyncCompletion.complete()
    } catch (exception: Exception) {
      asyncCompletion.completeExceptionally(exception)
    }
  }
  return asyncCompletion
}

/**
 * Converts this job to a [AsyncCompletion].
 * The job is cancelled when the returned [AsyncCompletion] is cancelled or otherwise completed.
 */
fun Job.asAsyncCompletion(): AsyncCompletion {
  val asyncCompletion = AsyncCompletion.incomplete()
  asyncCompletion.whenComplete { exception -> cancel(exception) }
  invokeOnCompletion {
    try {
      asyncCompletion.complete()
    } catch (exception: Exception) {
      asyncCompletion.completeExceptionally(exception)
    }
  }
  return asyncCompletion
}

/**
 * Converts this [AsyncCompletion] to an instance of [Deferred].
 * The [AsyncCompletion] is cancelled when the resulting deferred is cancelled.
 */
fun AsyncCompletion.asDeferred(): Deferred<Unit> {
  // Fast path if already completed
  if (isDone) {
    return try {
      CompletableDeferred(join())
    } catch (e: Throwable) {
      // unwrap original cause from CompletionException
      val original = (e as? CompletionException)?.cause ?: e
      CompletableDeferred<Unit>().also { it.completeExceptionally(original) }
    }
  }
  val result = CompletableDeferred<Unit>()
  whenComplete { exception ->
    if (exception == null) {
      result.complete(Unit)
    } else {
      result.completeExceptionally(exception)
    }
  }
  result.invokeOnCompletion { this.cancel() }
  return result
}

/**
 * Awaits for completion of the [AsyncCompletion] without blocking a thread.
 *
 * This suspending function is cancellable. If the [Job] of the current coroutine is cancelled or completed while this
 * suspending function is waiting, this function stops waiting for the [AsyncCompletion] and immediately resumes with
 * [CancellationException].
 *
 * Note, that [AsyncCompletion] does not support prompt removal of listeners, so on cancellation of this wait a few
 * small objects will remain in the [AsyncCompletion] stack of completion actions until it completes itself. However,
 * care is taken to clear the reference to the waiting coroutine itself, so that its memory can be released even if the
 * [AsyncCompletion] never completes.
 */
suspend fun AsyncCompletion.await() {
  // fast path when CompletableFuture is already done (does not suspend)
  if (isDone) {
    try {
      return join()
    } catch (e: CompletionException) {
      throw e.cause ?: e // unwrap original cause from CompletionException
    }
  }
  // slow path -- suspend
  return suspendCancellableCoroutine { cont: CancellableContinuation<Unit> ->
    val consumer = ContinuationConsumer(cont)
    whenComplete(consumer)
    cont.invokeOnCompletion {
      consumer.cont = null // shall clear reference to continuation
    }
  }
}

private class ContinuationConsumer(
  @Volatile @JvmField var cont: Continuation<Unit>?
) : Consumer<Throwable?> {
  override fun accept(exception: Throwable?) {
    val cont = this.cont ?: return // atomically read current value unless null
    if (exception == null) // the future has been completed normally
      cont.resume(Unit)
    else // the future has completed with an exception
      cont.resumeWithException(exception)
  }
}
