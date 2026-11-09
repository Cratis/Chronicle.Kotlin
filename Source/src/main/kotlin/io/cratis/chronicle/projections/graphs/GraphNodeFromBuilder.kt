// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import io.cratis.chronicle.projections.PropertyValidator
import kotlin.reflect.KClass

internal class GraphNodeFromBuilder<TNode : Any, TEvent : Any>(
    nodeClass: KClass<TNode>,
    eventClass: KClass<TEvent>
) : GraphMappingBuilder<TNode, IGraphNodeFromBuilder<TNode, TEvent>>(nodeClass, eventClass),
    IGraphNodeFromBuilder<TNode, TEvent> {
    /** The key expression, or `null` for the event source id. */
    var key: String? = null
        private set

    /** Whether the event only changes a node that exists. */
    var updateOnly: Boolean = false
        private set

    override val self: IGraphNodeFromBuilder<TNode, TEvent> get() = this

    override fun usingKey(eventPropertyName: String): IGraphNodeFromBuilder<TNode, TEvent> {
        key = PropertyValidator.validatePropertyExists(eventClass, eventPropertyName)
        return this
    }

    override fun onlyWhenExisting(): IGraphNodeFromBuilder<TNode, TEvent> {
        updateOnly = true
        return this
    }
}
