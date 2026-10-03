// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

/**
 * Optional capability of an event sequence: validates the event source routing of [AppendOptions]
 * without appending anything. Used to check several appends before the first of them is written.
 */
internal interface IEventSourceRoutingPreflight {
    /**
     * Validates the event source routing of [options].
     *
     * @throws io.cratis.chronicle.eventSources.UnknownEventSource The class is not a discovered definition.
     * @throws io.cratis.chronicle.eventSources.EventStreamDoesNotBelongToEventSource The stream is not declared.
     * @throws io.cratis.chronicle.eventSources.EventRoutingContradictsEventSource Explicit routing contradicts the definition.
     */
    fun preflightRouting(options: AppendOptions?)
}
