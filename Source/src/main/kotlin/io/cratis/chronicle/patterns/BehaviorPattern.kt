// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.patterns

import java.time.OffsetDateTime

/**
 * Behavior the kernel has established for a scope: what tends to happen in a given context.
 *
 * @param groupingKey The scope the pattern belongs to.
 * @param facets The context the pattern holds in, including the action when it names one.
 * @param occurrences How many times the pattern has been observed.
 * @param confidence How confident the kernel is in the pattern.
 * @param support How large a share of the scope's activity the pattern covers.
 * @param weight The weight the kernel ranks the pattern by.
 * @param firstSeen When the pattern was first observed, or `null` when the kernel did not say.
 * @param lastSeen When the pattern was last observed, or `null` when the kernel did not say.
 */
data class BehaviorPattern(
    val groupingKey: PatternGroupingKey,
    val facets: FacetSet,
    val occurrences: Long,
    val confidence: PatternConfidence,
    val support: Double,
    val weight: Double,
    val firstSeen: OffsetDateTime?,
    val lastSeen: OffsetDateTime?,
) {
    /** How many facets the pattern is constrained by. */
    val specificity: Int get() = facets.specificity

    /** The action the pattern names, or [FacetValue.Unspecified] when it names none. */
    val action: FacetValue get() = facets.action
}
