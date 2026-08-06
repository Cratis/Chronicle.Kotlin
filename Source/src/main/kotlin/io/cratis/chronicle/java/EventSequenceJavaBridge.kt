// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.java

import io.cratis.chronicle.eventSequences.AppendResult
import io.cratis.chronicle.eventSequences.EventToAppend
import io.cratis.chronicle.eventSequences.IEventSequence
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScope
import java.util.UUID
import kotlinx.coroutines.runBlocking

/**
 * Java-friendly bridge for the multi-event-source [IEventSequence.appendMany].
 *
 * Java can neither call a `suspend` function nor fill in Kotlin default arguments, so this blocks on
 * the coroutine and is `@JvmOverloads` to give Java the shorter forms.
 */
object EventSequenceJavaBridge {
    /**
     * Appends events spanning any number of event sources as a single atomic batch.
     *
     * @param eventSequence The event sequence to append to.
     * @param events The events to append, in the order they should be appended.
     * @param concurrencyScopes Optional [ConcurrencyScope] per event source id. Sources left out are
     *   appended without a concurrency check.
     * @param correlationId Optional correlation identifier for the whole batch.
     * @return A list of [AppendResult], one per event, in the order of [events].
     */
    @JvmStatic
    @JvmOverloads
    fun appendMany(
        eventSequence: IEventSequence,
        events: List<EventToAppend>,
        concurrencyScopes: Map<String, ConcurrencyScope> = emptyMap(),
        correlationId: UUID? = null
    ): List<AppendResult> = runBlocking { eventSequence.appendMany(events, concurrencyScopes, correlationId) }
}
