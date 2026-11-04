// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.events

import kotlin.reflect.KClass

/**
 * Marks an event type as a compensation for another event type: a new event that records that an
 * earlier event is being offset, such as a reversal entry in a ledger. The original event is never
 * changed.
 *
 * Chronicle stores the link on the event type's schema, as its `compensationFor` entry, and only
 * accepts a compensating event appended with
 * [compensate][io.cratis.chronicle.eventSequences.IEventSequence.compensate] when its type is
 * declared for the type of the event being compensated.
 *
 * ```kotlin
 * @EventType
 * data class EntryPosted(val amount: Long)
 *
 * @EventType
 * @CompensationFor(EntryPosted::class)
 * data class EntryReversed(val reason: String)
 * ```
 *
 * @property eventType The event type this event compensates for. It must itself be annotated with [EventType].
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class CompensationFor(val eventType: KClass<*>)
