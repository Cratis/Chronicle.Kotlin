// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.patterns

/**
 * One dimension of context: a [name] with a [value].
 *
 * @param name The name of the facet.
 * @param value The value of the facet.
 */
data class Facet(val name: FacetName, val value: FacetValue)
