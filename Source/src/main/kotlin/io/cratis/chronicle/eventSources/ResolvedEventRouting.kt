// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSources

import kotlin.reflect.KClass

/**
 * The routing of an append resolved through an [EventSourceDefinition].
 *
 * @property definition The event source the append goes through.
 * @property stream The stream within it, if the append names one.
 */
internal data class ResolvedEventRouting(val definition: EventSourceDefinition, val stream: EventStreamDefinition?) {
    /** The event source type written on the event. */
    val eventSourceType: String get() = definition.name

    /** The event stream type written on the event, or `null` to leave the kernel default. */
    val eventStreamType: String? get() = stream?.name

    /** The dimensions that apply to the append's concurrency check. */
    val dimensions: Set<ConcurrencyDimension> get() = definition.concurrencyFor(stream)

    companion object {
        private val unspecifiedSourceTypes = setOf("", "Default")
        private val unspecifiedStreamTypes = setOf("", "Default", "All")

        /**
         * Resolves routing through the event source [type].
         *
         * @param eventSources The known definitions.
         * @param type The class carrying the definition.
         * @param stream The stream name, if any.
         * @param explicitEventSourceType An explicit event source type from the caller, if any.
         * @param explicitEventStreamType An explicit event stream type from the caller, if any.
         * @throws UnknownEventSource The type is not a discovered event source.
         * @throws EventStreamDoesNotBelongToEventSource The stream is not declared by the event source.
         * @throws EventRoutingContradictsEventSource Explicit routing contradicts the definition.
         */
        fun resolve(
            eventSources: IEventSources,
            type: KClass<*>,
            stream: String?,
            explicitEventSourceType: String?,
            explicitEventStreamType: String?
        ): ResolvedEventRouting {
            val definition = eventSources.getFor(type)

            if (explicitEventSourceType != null &&
                explicitEventSourceType !in unspecifiedSourceTypes &&
                explicitEventSourceType != definition.name
            ) {
                throw EventRoutingContradictsEventSource(definition.name, "event source type", definition.name, explicitEventSourceType)
            }

            val explicitStream = explicitEventStreamType?.takeIf { it !in unspecifiedStreamTypes }
            if (stream != null && explicitStream != null && stream != explicitStream) {
                throw EventRoutingContradictsEventSource(definition.name, "event stream type", stream, explicitStream)
            }

            val streamName = stream ?: explicitStream ?: return ResolvedEventRouting(definition, null)
            val eventStream = definition.findStream(streamName)
                ?: throw EventStreamDoesNotBelongToEventSource(definition.name, streamName)
            return ResolvedEventRouting(definition, eventStream)
        }
    }
}
