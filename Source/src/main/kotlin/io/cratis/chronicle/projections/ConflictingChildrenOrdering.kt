// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

/**
 * Thrown when the [ChildrenFrom] annotations on a property declare different orderings for the same
 * children collection.
 *
 * @param propertyName The children property with conflicting orderings.
 */
class ConflictingChildrenOrdering(
    propertyName: String,
) : IllegalArgumentException(
    "The children property '$propertyName' declares more than one ordering. Use only one of prepend, orderBy or " +
        "orderByDescending, and declare the same ordering on every @ChildrenFrom of the property.",
)
