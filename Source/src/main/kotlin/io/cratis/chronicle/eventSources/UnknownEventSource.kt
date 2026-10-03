// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSources

/** Thrown when an append names an event source that is not a discovered definition. */
class UnknownEventSource(val eventSource: String) : RuntimeException(
    "'$eventSource' is not a known event source. Annotate the class with @EventSource and make sure it is discovered."
)
