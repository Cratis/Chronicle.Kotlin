// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.java

import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.AppendResult
import io.cratis.chronicle.eventSequences.ITransactionalEventSequence
import kotlinx.coroutines.runBlocking

/**
 * The transactional view of an event sequence, for Java, without the coroutines.
 *
 * Appends here are staged against the current unit of work rather than committed one at a time.
 *
 * @param sequence The transactional sequence to forward to.
 */
class BlockingTransactionalEventSequence(private val sequence: ITransactionalEventSequence) {

    /** The suspending sequence underneath. */
    fun unwrap(): ITransactionalEventSequence = sequence

    /** Stages [event] against [eventSourceId]. */
    @JvmOverloads
    fun append(eventSourceId: String, event: Any, options: AppendOptions? = null): AppendResult =
        runBlocking { sequence.append(eventSourceId, event, options) }

    /** Stages [events] against [eventSourceId]. */
    @JvmOverloads
    fun appendMany(
        eventSourceId: String,
        events: List<Any>,
        options: AppendOptions? = null
    ): List<AppendResult> = runBlocking { sequence.appendMany(eventSourceId, events, options) }
}
