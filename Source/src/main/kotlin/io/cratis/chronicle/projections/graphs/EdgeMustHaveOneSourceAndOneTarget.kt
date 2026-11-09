// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import kotlin.reflect.KClass

/**
 * Thrown when an edge class does not have exactly one property marked [EdgeSource] and one marked [EdgeTarget].
 *
 * @param type The edge class.
 */
class EdgeMustHaveOneSourceAndOneTarget(type: KClass<*>) : IllegalArgumentException(
    "The edge type '${type.displayName()}' must have exactly one property marked as source and one marked as target."
)
