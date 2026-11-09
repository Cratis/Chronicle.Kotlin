// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

/**
 * Thrown at registration when a projection orders its children but the Chronicle contracts this client is
 * built against cannot carry the ordering to the kernel.
 *
 * Children ordering travels on `ChildrenDefinition.ordering`, which the published
 * `io.cratis:chronicle-contracts` package gains with the Chronicle release that adds ordered children.
 * Until the client depends on that version, registering an ordering fails here rather than silently
 * appending children in arrival order.
 *
 * @param propertyName The children property that declares an ordering.
 */
class ChildrenOrderingNotSupportedByContracts(
    propertyName: String,
) : UnsupportedOperationException(
    "The children property '$propertyName' declares an ordering, but the Chronicle contracts this client uses " +
        "cannot carry children ordering yet. Remove the ordering, or upgrade to a client version built against " +
        "Chronicle contracts that include ChildrenOrderingDefinition.",
)
