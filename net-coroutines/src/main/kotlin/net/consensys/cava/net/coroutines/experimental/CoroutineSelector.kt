/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.cava.net.coroutines.experimental

import com.google.common.util.concurrent.ThreadFactoryBuilder
import kotlinx.coroutines.experimental.CancellableContinuation
import kotlinx.coroutines.experimental.suspendCancellableCoroutine
import org.logl.LogMessage
import org.logl.LoggerProvider
import java.nio.channels.CancelledKeyException
import java.nio.channels.ClosedChannelException
import java.nio.channels.ClosedSelectorException
import java.nio.channels.SelectableChannel
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * A common co-routine selector pool.
 *
 * This pool is created with a selector per available processors.
 */
val CommonCoroutineSelector: CoroutineSelector = CoroutineSelectorPool.open(Runtime.getRuntime().availableProcessors())

/**
 * A selector for coroutine-based channel IO.
 */
interface CoroutineSelector {

  /**
   * Wait for a channel to become ready for any of the specified operations.
   *
   * @param channel The channel.
   * @param ops The interest set, as a combination of [SelectionKey.OP_ACCEPT], [SelectionKey.OP_CONNECT],
   *   [SelectionKey.OP_READ] and/or [SelectionKey.OP_WRITE].
   * @throws ClosedSelectorException If the co-routine selector has been closed.
   */
  suspend fun select(channel: SelectableChannel, ops: Int)

  /**
   * Cancel any suspended calls to [select] for the specified channel.
   *
   * @param channel The channel.
   * @param cause An optional cause for the cancellation.
   * @return `true` if any suspensions were cancelled.
   * @throws ClosedSelectorException If the co-routine selector has been closed.
   */
  suspend fun cancelSelections(channel: SelectableChannel, cause: Throwable? = null): Boolean

  /**
   * Force the selection loop, if running, to wake up and process any closed channels.
   *
   * @throws ClosedSelectorException If the co-routine selector has been closed.
   */
  fun wakeup()

  /**
   * Close the co-routine selector.
   */
  fun close()
}

/**
 * A pool of co-routine selectors.
 */
class CoroutineSelectorPool private constructor(
  poolSize: Int,
  private val executor: Executor,
  loggerProvider: LoggerProvider,
  private val selectTimeout: Long,
  private val idleTasks: Int
) : CoroutineSelector {

  companion object {
    /**
     * Open a co-routine selection pool.
     *
     * @param poolSize The number of selectors in the pool.
     * @param executor An executor for obtaining threads to run the selection loop of each selector.
     * @param loggerProvider A provider for logger instances.
     * @param selectTimeout The maximum time the selection operation will wait before checking for closed channels.
     * @param idleTimeout The minimum idle time before the selection loop of a selector exits.
     * @return A co-routine selection pool.
     */
    fun open(
      poolSize: Int,
      executor: Executor = Executors.newFixedThreadPool(poolSize,
        ThreadFactoryBuilder().setNameFormat("selection-dispatch-%d").build()),
      loggerProvider: LoggerProvider = LoggerProvider.nullProvider(),
      selectTimeout: Long = 1000,
      idleTimeout: Long = 10000
    ): CoroutineSelector {
      require(poolSize > 0) { "poolSize must be larger than zero" }
      require(selectTimeout > 0) { "selectTimeout must be larger than zero" }
      require(idleTimeout >= 0) { "idleTimeout must be positive" }
      val idleTasks = idleTimeout / selectTimeout
      require(idleTasks <= Integer.MAX_VALUE) { "idleTimeout is too large" }

      return if (poolSize == 1) {
        SingleCoroutineSelector(executor, Selector.open(), loggerProvider, selectTimeout, idleTasks.toInt())
      } else {
        CoroutineSelectorPool(poolSize, executor, loggerProvider, selectTimeout, idleTasks.toInt())
      }
    }
  }

  private val selectors: Array<CoroutineSelector> = Array(poolSize) {
    SingleCoroutineSelector(executor, Selector.open(), loggerProvider, selectTimeout, idleTasks)
  }

  override suspend fun select(channel: SelectableChannel, ops: Int) {
    selectors[System.identityHashCode(channel).rem(selectors.size)].select(channel, ops)
  }

  override suspend fun cancelSelections(channel: SelectableChannel, cause: Throwable?): Boolean {
    return selectors[System.identityHashCode(channel).rem(selectors.size)].cancelSelections(channel)
  }

  override fun wakeup() {
    selectors.forEach { it.wakeup() }
  }

  override fun close() {
    selectors.forEach { it.close() }
  }

  private class SingleCoroutineSelector(
    private val executor: Executor,
    private val selector: Selector,
    loggerProvider: LoggerProvider,
    private val selectTimeout: Long,
    private val idleTasks: Int
  ) : CoroutineSelector {

    private val logger = loggerProvider.getLogger(CoroutineSelectorPool::class.java)

    private val pendingInterests = ConcurrentLinkedQueue<SelectionInterest>()
    private val pendingCancellations = ConcurrentLinkedQueue<SelectionCancellation>()
    private val outstandingTasks = AtomicInteger(0)
    private val registeredKeys = HashSet<SelectionKey>()

    init {
      require(selector.isOpen) { "Selector is closed" }
      require(selector.keys().isEmpty()) { "Selector already has selection keys" }
    }

    override suspend fun select(channel: SelectableChannel, ops: Int) {
      require(!channel.isBlocking) { "AsyncChannel must be set to non blocking" }
      require(ops != 0) { "ops must not be zero" }
      require(ops and channel.validOps().inv() == 0) { "Invalid operations for channel" }
      if (!selector.isOpen) {
        throw ClosedSelectorException()
      }
      suspendCancellableCoroutine { cont: CancellableContinuation<Unit> ->
        // increment tasks first to keep selection loop running while we add a new pending interest
        val isRunning = incrementTasks()
        pendingInterests.add(SelectionInterest(cont, channel, ops))
        wakeup(isRunning)
      }
    }

    override suspend fun cancelSelections(channel: SelectableChannel, cause: Throwable?): Boolean {
      if (!selector.isOpen) {
        throw ClosedSelectorException()
      }
      check(selector.isOpen) { "Selector is closed" }
      return suspendCancellableCoroutine { cont: CancellableContinuation<Boolean> ->
        // increment tasks first to keep selection loop running while we add a new pending cancellation
        val isRunning = incrementTasks()
        pendingCancellations.add(SelectionCancellation(channel, cause, cont))
        wakeup(isRunning)
      }
    }

    override fun wakeup() {
      if (!selector.isOpen) {
        throw ClosedSelectorException()
      }
      selector.wakeup()
    }

    override fun close() {
      selector.close()
    }

    private fun incrementTasks(): Boolean = outstandingTasks.getAndIncrement() != 0

    private fun wakeup(isRunning: Boolean) {
      if (isRunning) {
        logger.debug("Selector {}: Interrupting selection loop", System.identityHashCode(selector))
        selector.wakeup()
      } else {
        executor.execute(this::selectionLoop)
      }
    }

    private fun selectionLoop() {
      logger.debug("Selector {}: Starting selection loop", System.identityHashCode(selector))
      try {
        // allow the selector to cleanup any outstanding cancelled keys before starting the loop
        selector.selectNow()
        outstandingTasks.addAndGet(idleTasks)
        var idleCount = 0

        while (true) {
          // add pending selections before awakening selected, which allows for newly added
          // selections to be awoken immediately and avoids trying to register to already canceled keys
          if (processTasks(this::registerPendingSelections)) {
            break
          }
          if (processTasks(this::processPendingCancellations)) {
            break
          }
          if (processTasks(this::awakenSelected)) {
            break
          }

          if (selector.keys().isEmpty()) {
            if (outstandingTasks.decrementAndGet() == 0) {
              break
            }
            idleCount++
          } else {
            outstandingTasks.addAndGet(idleCount)
            idleCount = 0
          }

          selector.selectedKeys().clear()
          // use a timeout on select, as keys cancelled via channel close wont wakeup the selector
          selector.select(selectTimeout)

          if (!selector.isOpen) {
            cancelAll(ClosedSelectorException())
            break
          }
          if (processTasks(this::cancelMissingRegistrations)) {
            break
          }
        }
        logger.debug("Selector {}: Exiting selection loop", System.identityHashCode(selector))
      } catch (e: Throwable) {
        selector.close()
        logger.error(LogMessage.patternFormat("Selector {}: An unexpected exception occurred in selection loop",
          System.identityHashCode(selector)), e)
        cancelAll(e)
      }
    }

    private fun processTasks(block: () -> Int): Boolean {
      val processed = block()
      val remaining = outstandingTasks.addAndGet(-processed)
      check(remaining >= 0) { "More tasks processed than were outstanding" }
      return remaining == 0
    }

    private fun registerPendingSelections(): Int {
      var processed = 0
      while (true) {
        val interest = pendingInterests.poll() ?: break
        try {
          val key = interest.channel.keyFor(selector)
          val registered = if (key == null) {
            registerInterest(interest)
          } else {
            mergeInterest(key, interest)
          }
          if (!registered) {
            processed++
          }
        } catch (e: Throwable) {
          interest.cont.cancel(e)
          throw e
        }
      }
      return processed
    }

    private fun registerInterest(interest: SelectionInterest): Boolean {
      val key: SelectionKey
      try {
        key = interest.channel.register(selector, interest.ops, arrayListOf(interest))
      } catch (e: ClosedChannelException) {
        interest.cont.resumeWithException(e)
        return false
      }
      registeredKeys.add(key)
      logger.debug("Selector {}: Registered {}@{} for interests {}", System.identityHashCode(selector),
        interest.channel, System.identityHashCode(interest.channel), interest.ops)
      return true
    }

    private fun mergeInterest(key: SelectionKey, interest: SelectionInterest): Boolean {
      val mergedInterests: Int
      try {
        mergedInterests = key.interestOps() or interest.ops
        key.interestOps(mergedInterests)
      } catch (e: CancelledKeyException) {
        // key must have been cancelled via closing the channel
        val exception = ClosedChannelException()
        exception.addSuppressed(e)
        interest.cont.resumeWithException(exception)
        return false
      }
      @Suppress("UNCHECKED_CAST")
      val interests = key.attachment() as ArrayList<SelectionInterest>
      interests.add(interest)
      logger.debug("Selector {}: Updated registration for channel {} to interests {}",
        System.identityHashCode(selector), System.identityHashCode(interest.channel), mergedInterests)
      return true
    }

    private fun processPendingCancellations(): Int {
      var processed = 0
      while (true) {
        val cancellation = pendingCancellations.poll() ?: break
        processed++
        val key = cancellation.channel.keyFor(selector)
        if (key != null) {
          logger.debug("Selector {}: Cancelling registration for channel {}", System.identityHashCode(selector),
            System.identityHashCode(cancellation.channel))

          @Suppress("UNCHECKED_CAST")
          val interests = key.attachment() as ArrayList<SelectionInterest>
          for (interest in interests) {
            interest.cont.cancel(cancellation.cause)
            processed++
          }
          interests.clear()
          key.cancel()
          registeredKeys.remove(key)
          cancellation.cont.resume(true)
        } else {
          cancellation.cont.resume(false)
        }
      }
      return processed
    }

    private fun awakenSelected(): Int {
      var awoken = 0
      val selectedKeys = selector.selectedKeys()
      for (key in selectedKeys) {
        @Suppress("UNCHECKED_CAST")
        val interests = key.attachment() as ArrayList<SelectionInterest>

        if (!key.isValid) {
          // channel must have been closed
          val cause = ClosedChannelException()
          interests.forEach { it.cont.cancel(cause) }
          interests.clear()
          continue
        }

        val readyOps = key.readyOps()
        logger.debug("Selector {}: Channel {} selected for interests {}", System.identityHashCode(selector),
          System.identityHashCode(key.channel()), readyOps)
        var remainingOps = 0

        val it = interests.iterator()
        while (it.hasNext()) {
          val interest = it.next()
          // if any of the interests are set, then resume the continuation
          if ((interest.ops and readyOps) != 0) {
            interest.cont.resume(Unit)
            it.remove()
            awoken++
          } else {
            remainingOps = remainingOps or interest.ops
          }
        }
        key.interestOps(remainingOps)
        if (interests.isEmpty()) {
          registeredKeys.remove(key)
          key.cancel()
        }
      }
      return awoken
    }

    private fun cancelMissingRegistrations(): Int {
      val selectorKeys = selector.keys()
      if (selectorKeys.size == registeredKeys.size) {
        // assume sets of the same size contain the same members
        return 0
      }

      // There should only be less keys in the selector, as keys will vanish after cancellation via closing their channel
      check(selectorKeys.size < registeredKeys.size) { "More registered keys than are outstanding" }

      var processed = 0
      registeredKeys.removeIf { key ->
        if (selectorKeys.contains(key)) {
          false
        } else {
          val cause = ClosedChannelException()
          @Suppress("UNCHECKED_CAST")
          val interests = key.attachment() as ArrayList<SelectionInterest>
          for (interest in interests) {
            interest.cont.cancel(cause)
            processed++
          }
          interests.clear()
          key.cancel()
          true
        }
      }
      return processed
    }

    private fun cancelAll(e: Throwable) {
      pendingInterests.forEach { it.cont.cancel(e) }
      pendingInterests.clear()
      registeredKeys.forEach { key ->
        @Suppress("UNCHECKED_CAST")
        (key.attachment() as ArrayList<SelectionInterest>).forEach { it.cont.cancel(e) }
      }
      registeredKeys.clear()
      pendingCancellations.clear()
      outstandingTasks.set(0)
    }
  }

  private data class SelectionInterest(
    val cont: CancellableContinuation<Unit>,
    val channel: SelectableChannel,
    val ops: Int
  )

  private data class SelectionCancellation(
    val channel: SelectableChannel,
    val cause: Throwable?,
    val cont: CancellableContinuation<Boolean>
  )
}
