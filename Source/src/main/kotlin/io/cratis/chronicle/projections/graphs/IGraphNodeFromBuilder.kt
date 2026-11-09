// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

/** Implemented by the client; applications should not implement this builder interface. */
interface IGraphNodeFromBuilder<TNode : Any, TEvent : Any> :
    IGraphMappingBuilder<TNode, IGraphNodeFromBuilder<TNode, TEvent>> {
    /**
     * Uses the event property [eventPropertyName] as the key of the node, instead of the event source id.
     *
     * @throws io.cratis.chronicle.projections.InvalidPropertyForType when the event has no such property.
     */
    fun usingKey(eventPropertyName: String): IGraphNodeFromBuilder<TNode, TEvent>

    /** Only changes the node when it already exists; the event never creates it. */
    fun onlyWhenExisting(): IGraphNodeFromBuilder<TNode, TEvent>
}
