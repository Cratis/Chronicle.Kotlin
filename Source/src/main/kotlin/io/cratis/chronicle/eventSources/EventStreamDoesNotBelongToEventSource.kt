// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSources

/** Thrown when an append names a stream the event source does not declare. */
class EventStreamDoesNotBelongToEventSource(val eventSource: String, val stream: String) : RuntimeException(
    "The stream '$stream' is not declared by the event source '$eventSource'."
)
