// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSources

/**
 * Declares a stream of an event source, as part of [EventSource.streams].
 *
 * A stream is a named route within an event source - routing belongs to the append, not to the
 * event type, so any event can be appended to any declared stream.
 *
 * @property name The stable name of the stream. Unique within the event source.
 * @property description A human readable description of the stream.
 * @property concurrency The dimensions that take part in concurrency checks for appends to this
 *   stream. Falls back to the event source's dimensions when none are declared.
 */
@Target()
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class EventStream(
    val name: String,
    val description: String = "",
    val concurrency: Array<ConcurrencyDimension> = []
)
