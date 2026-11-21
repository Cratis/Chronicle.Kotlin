// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.patterns

/**
 * A partial context: a set of [Facet]s with at most one value per name. Immutable - [with] returns a new set.
 *
 * Facets without a value are dropped, and the facets are kept ordered by name so two sets with the same
 * facets are equal regardless of how they were built.
 *
 * @param facets The facets; the first one for a name wins and unspecified values are dropped.
 */
class FacetSet(facets: Iterable<Facet>) {
    /** The facets in the set, ordered by name. */
    val facets: List<Facet> = facets
        .filter { it.value.isSpecified }
        .distinctBy { it.name }
        .sortedBy { it.name.value }

    /** How many facets the set constrains. */
    val specificity: Int get() = facets.size

    /** Whether the set constrains nothing. */
    val isEmpty: Boolean get() = facets.isEmpty()

    /** The action the set names, or [FacetValue.Unspecified] when it names none. */
    val action: FacetValue get() = facets.firstOrNull { it.name.isAction }?.value ?: FacetValue.Unspecified

    /** A set with [name] set to [value], replacing any value this set had for [name]. */
    fun with(name: FacetName, value: FacetValue): FacetSet =
        FacetSet(facets.filter { it.name != name } + Facet(name, value))

    /** A set with [name] set to [value], replacing any value this set had for [name]. */
    fun with(name: FacetName, value: String): FacetSet = with(name, FacetValue(value))

    /** The value of [name], or [FacetValue.Unspecified] when the set does not constrain it. */
    fun valueOf(name: FacetName): FacetValue =
        facets.firstOrNull { it.name == name }?.value ?: FacetValue.Unspecified

    /** Whether the set constrains [name]. */
    fun constrains(name: FacetName): Boolean = facets.any { it.name == name }

    /** The facets as a name to value map. */
    fun asMap(): Map<FacetName, FacetValue> = facets.associate { it.name to it.value }

    override fun equals(other: Any?): Boolean = other is FacetSet && facets == other.facets

    override fun hashCode(): Int = facets.hashCode()

    override fun toString(): String = facets.joinToString(";") { "${it.name}=${it.value}" }

    companion object {
        /** A set that constrains nothing. */
        @JvmField val empty = FacetSet(emptyList())
    }
}
