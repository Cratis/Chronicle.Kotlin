// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSources

/**
 * Marks a class as the definition of a registered event source.
 *
 * The class is only a carrier for the definition - it is never instantiated. The definition is
 * discovered with the other client artifacts, registered with the event store on startup, and
 * can then be named in an append:
 *
 * ```kotlin
 * @EventSource(
 *     name = "Account",
 *     description = "A customer account",
 *     concurrency = [ConcurrencyDimension.EventSourceId],
 *     streams = [EventStream("Transactions", "Money in and out")]
 * )
 * class AccountEventSource
 *
 * eventLog.appendThroughEventSource(AccountEventSource::class, accountId, deposited, eventStream = "Transactions")
 * ```
 *
 * Definitions are optional. Appending without one behaves exactly as it always has.
 *
 * @property name The stable name of the event source. Defaults to the class name without an
 *   `EventSource` suffix.
 * @property description A human readable description of the event source.
 * @property concurrency The default dimensions that take part in concurrency checks for appends
 *   through this event source.
 * @property streams The streams the event source declares.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class EventSource(
    val name: String = "",
    val description: String = "",
    val concurrency: Array<ConcurrencyDimension> = [],
    val streams: Array<EventStream> = []
)
