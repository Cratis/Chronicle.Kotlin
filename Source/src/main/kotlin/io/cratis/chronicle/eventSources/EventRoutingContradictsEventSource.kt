// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSources

/**
 * Thrown when explicit routing on an append contradicts the event source definition it goes through,
 * for example an event source type that is not the definition's name.
 */
class EventRoutingContradictsEventSource(
    val eventSource: String,
    val dimension: String,
    val expected: String,
    val actual: String
) : RuntimeException(
    "The explicit $dimension '$actual' contradicts the event source '$eventSource', which resolves it to '$expected'."
)
