// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import kotlin.reflect.KClass

/**
 * Thrown when a children builder is given a second ordering - only one of `prepend`, `orderBy` or
 * `orderByDescending` applies to a collection.
 *
 * @param childClass The type of the children whose ordering was defined twice.
 */
class ChildrenOrderingAlreadyDefined(
    childClass: KClass<*>,
) : IllegalStateException(
    "An ordering is already defined for the children of type '${childClass.qualifiedName}'. " +
        "Use one of prepend, orderBy or orderByDescending.",
)
