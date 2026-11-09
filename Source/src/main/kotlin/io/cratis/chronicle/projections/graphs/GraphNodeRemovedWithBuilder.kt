// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import io.cratis.chronicle.projections.PropertyValidator
import kotlin.reflect.KClass

internal class GraphNodeRemovedWithBuilder<TEvent : Any>(
    private val eventClass: KClass<TEvent>
) : IGraphNodeRemovedWithBuilder<TEvent> {
    /** The key expression, or `null` for the event source id. */
    var key: String? = null
        private set

    /** Whether the edges attached to the node are deleted with it. */
    var detachEdges: Boolean = false
        private set

    override fun usingKey(eventPropertyName: String): IGraphNodeRemovedWithBuilder<TEvent> {
        key = PropertyValidator.validatePropertyExists(eventClass, eventPropertyName)
        return this
    }

    override fun detach(): IGraphNodeRemovedWithBuilder<TEvent> {
        detachEdges = true
        return this
    }
}
