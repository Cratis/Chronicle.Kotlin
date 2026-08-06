// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.readModels

import io.cratis.chronicle.eventSequences.IEventLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

/**
 * Registers [IReadModelReactor] instances against [IReadModelsService.watch].
 *
 * Registration is not a suspending call - it starts background subscriptions and returns - so Java
 * callers can use this directly without going through a bridge.
 *
 * ```kotlin
 * val readModelReactors = ReadModelReactors(store.readModels, store.eventLog)
 * readModelReactors.register(EmployeeAlerts())
 * ```
 *
 * @param readModels The read models to watch through.
 * @param eventLog The event log any side-effect events returned by a handler are appended to.
 */
class ReadModelReactors(
    private val readModels: IReadModelsService,
    eventLog: IEventLog
) : IReadModelReactors {
    private val sideEffects = ReadModelReactorSideEffects(eventLog)
    private val jobs = mutableListOf<Job>()

    override fun register(reactor: IReadModelReactor): Job {
        // Resolved before anything is launched, so a malformed handler surfaces to the caller here
        // rather than inside a coroutine nobody is watching.
        val handlers = ReadModelReactorHandlers.from(reactor::class)

        val job = CoroutineScope(Dispatchers.IO).launch {
            for (readModelClass in handlers.readModelClasses) {
                launch { keepWatching(reactor, handlers, readModelClass) }
            }
        }

        jobs.add(job)
        return job
    }

    override fun stop() {
        jobs.forEach { it.cancel() }
        jobs.clear()
    }

    /**
     * Collects changes for [readModelClass] until cancelled, re-establishing the watch whenever it
     * ends. The kernel closes a watch stream rather than tailing it forever, so a stream that ends
     * cleanly still has to be picked up again or the reactor quietly stops reacting.
     */
    private suspend fun CoroutineScope.keepWatching(
        reactor: IReadModelReactor,
        handlers: ReadModelReactorHandlers,
        readModelClass: KClass<*>
    ) {
        while (isActive) {
            try {
                @Suppress("UNCHECKED_CAST")
                readModels.watch(readModelClass as KClass<Any>).collect { changeset ->
                    dispatch(reactor, handlers, readModelClass, changeset)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                report(reactor, readModelClass, e)
            }

            delay(REWATCH_DELAY_MS)
        }
    }

    /**
     * Runs every handler that matches the change. One handler throwing must not cost the others
     * their turn, nor tear down the watch, so failures are reported per handler and collection
     * carries on.
     */
    private suspend fun dispatch(
        reactor: IReadModelReactor,
        handlers: ReadModelReactorHandlers,
        readModelClass: KClass<*>,
        changeset: ReadModelChangeset<*>
    ) {
        for (method in handlers.resolve(readModelClass, changeset.changeType)) {
            try {
                sideEffects.append(method.invoke(reactor, changeset), changeset.modelKey)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                report(reactor, readModelClass, e)
            }
        }
    }

    private fun report(reactor: IReadModelReactor, readModelClass: KClass<*>, error: Exception) =
        System.err.println(
            "[ReadModelReactors] '${reactor::class.simpleName}' failed on " +
                "'${readModelClass.simpleName}': ${error.message}"
        )

    private companion object {
        /** How long to wait before re-establishing a watch whose stream ended. */
        const val REWATCH_DELAY_MS = 2_000L
    }
}
