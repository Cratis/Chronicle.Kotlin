// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

import io.cratis.chronicle.events.EventContext

/**
 * Pairs an event appended through a specific [IEventSequence] instance with the [AppendResult] of
 * that append operation. Emitted on [IEventSequence.appendOperations] after every completed append,
 * whether it succeeded or failed.
 *
 * @property context The [EventContext] the event was appended with.
 * @property event The event object that was appended.
 * @property result The [AppendResult] of the append.
 */
data class AppendedEventWithResult(
    val context: EventContext,
    val event: Any,
    val result: AppendResult
)
