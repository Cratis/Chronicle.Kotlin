// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import kotlin.reflect.KClass

internal class GraphEdgeFromBuilder<TEdge : Any, TEvent : Any>(
    edgeClass: KClass<TEdge>,
    eventClass: KClass<TEvent>
) : GraphMappingBuilder<TEdge, IGraphEdgeFromBuilder<TEdge, TEvent>>(edgeClass, eventClass),
    IGraphEdgeFromBuilder<TEdge, TEvent> {
    /** The source key expression, or `null` for the event source id. */
    var sourceKey: String? = null
        private set

    /** The target key expression, or `null` for the event source id. */
    var targetKey: String? = null
        private set

    /** The identity expression, or `null` to take it from the event property named like the identifying property. */
    var identity: String? = null
        private set

    override val self: IGraphEdgeFromBuilder<TEdge, TEvent> get() = this

    override fun usingSourceKey(eventPropertyName: String): IGraphEdgeFromBuilder<TEdge, TEvent> {
        sourceKey = eventProperty(eventPropertyName)
        return this
    }

    override fun usingTargetKey(eventPropertyName: String): IGraphEdgeFromBuilder<TEdge, TEvent> {
        targetKey = eventProperty(eventPropertyName)
        return this
    }

    override fun usingIdentity(eventPropertyName: String): IGraphEdgeFromBuilder<TEdge, TEvent> {
        identity = eventProperty(eventPropertyName)
        return this
    }
}
