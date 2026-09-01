// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.constraints

import kotlin.reflect.KClass

interface IConstraintsService {
    /** Registers every hand-written [IConstraint] in [constraints]. Anything else is ignored. */
    suspend fun register(vararg constraints: Any)

    /**
     * Registers every model-bound [Unique]/[RemoveConstraint] declared directly on [eventTypes] -
     * the declarative alternative to a hand-written [IConstraint].
     *
     * @param eventTypes Every class annotated [io.cratis.chronicle.events.EventType] the application owns.
     */
    suspend fun registerModelBound(eventTypes: List<KClass<*>>)
}
