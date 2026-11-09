// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

/** Implemented by the client; applications should not implement this builder interface. */
interface IGraphEdgeFromBuilder<TEdge : Any, TEvent : Any> :
    IGraphMappingBuilder<TEdge, IGraphEdgeFromBuilder<TEdge, TEvent>> {
    /**
     * Uses the event property [eventPropertyName] as the key of the source node, instead of the event source id.
     *
     * @throws io.cratis.chronicle.projections.InvalidPropertyForType when the event has no such property.
     */
    fun usingSourceKey(eventPropertyName: String): IGraphEdgeFromBuilder<TEdge, TEvent>

    /**
     * Uses the event property [eventPropertyName] as the key of the target node, instead of the event source id.
     *
     * @throws io.cratis.chronicle.projections.InvalidPropertyForType when the event has no such property.
     */
    fun usingTargetKey(eventPropertyName: String): IGraphEdgeFromBuilder<TEdge, TEvent>

    /**
     * Uses the event property [eventPropertyName] as the identity of the edge.
     *
     * @throws io.cratis.chronicle.projections.InvalidPropertyForType when the event has no such property.
     */
    fun usingIdentity(eventPropertyName: String): IGraphEdgeFromBuilder<TEdge, TEvent>
}
