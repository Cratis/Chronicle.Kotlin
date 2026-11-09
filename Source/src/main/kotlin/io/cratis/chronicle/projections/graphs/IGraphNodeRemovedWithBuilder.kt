// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

/** Implemented by the client; applications should not implement this builder interface. */
interface IGraphNodeRemovedWithBuilder<TEvent : Any> {
    /**
     * Uses the event property [eventPropertyName] as the key of the node to delete, instead of the event
     * source id.
     *
     * @throws io.cratis.chronicle.projections.InvalidPropertyForType when the event has no such property.
     */
    fun usingKey(eventPropertyName: String): IGraphNodeRemovedWithBuilder<TEvent>

    /** Deletes the edges attached to the node together with it. */
    fun detach(): IGraphNodeRemovedWithBuilder<TEvent>
}
