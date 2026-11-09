// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import io.cratis.chronicle.projections.PropertyValidator
import kotlin.reflect.KClass

internal class GraphEdgeRemovedWithBuilder<TEvent : Any>(
    private val eventClass: KClass<TEvent>
) : IGraphEdgeRemovedWithBuilder<TEvent> {
    /** The source key expression, or `null` for the event source id. */
    var sourceKey: String? = null
        private set

    /** The target key expression, or `null` for the event source id. */
    var targetKey: String? = null
        private set

    /** The identity expression, or `null` to take it from the event property named like the identifying property. */
    var identity: String? = null
        private set

    override fun usingSourceKey(eventPropertyName: String): IGraphEdgeRemovedWithBuilder<TEvent> {
        sourceKey = PropertyValidator.validatePropertyExists(eventClass, eventPropertyName)
        return this
    }

    override fun usingTargetKey(eventPropertyName: String): IGraphEdgeRemovedWithBuilder<TEvent> {
        targetKey = PropertyValidator.validatePropertyExists(eventClass, eventPropertyName)
        return this
    }

    override fun usingIdentity(eventPropertyName: String): IGraphEdgeRemovedWithBuilder<TEvent> {
        identity = PropertyValidator.validatePropertyExists(eventClass, eventPropertyName)
        return this
    }
}
