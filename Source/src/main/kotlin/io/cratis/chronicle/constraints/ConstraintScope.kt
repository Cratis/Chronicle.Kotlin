// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.constraints

/**
 * Represents the scope a constraint's uniqueness check applies within.
 *
 * By default a constraint is checked globally across the whole event store. Enabling a dimension
 * narrows the check to be per distinct value of that dimension instead - for example,
 * [perEventSourceType] means the constraint is only checked for uniqueness within each event
 * source type, rather than across all of them.
 *
 * @property perEventSourceType Whether the constraint is scoped per event source type.
 * @property perEventStreamType Whether the constraint is scoped per event stream type.
 * @property perEventStreamId Whether the constraint is scoped per event stream id.
 */
data class ConstraintScope(
    val perEventSourceType: Boolean = false,
    val perEventStreamType: Boolean = false,
    val perEventStreamId: Boolean = false
) {
    companion object {
        /** A constraint scope with no dimensions enabled - the constraint is checked globally. */
        val global: ConstraintScope = ConstraintScope()
    }

    /** Whether any scoping dimension is enabled. */
    val isScoped: Boolean get() = perEventSourceType || perEventStreamType || perEventStreamId
}
