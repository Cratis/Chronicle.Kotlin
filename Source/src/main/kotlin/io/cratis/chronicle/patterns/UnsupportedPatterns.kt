// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.patterns

import java.time.OffsetDateTime

/** The [IPatterns] of an event store implementation that predates, or opts out of, behavior patterns. */
internal object UnsupportedPatterns : IPatterns {
    override suspend fun getPatterns(
        groupingKey: PatternGroupingKey,
        context: FacetSet,
        minimumConfidence: PatternConfidence?,
        maximumResults: Int,
    ): List<BehaviorPattern> = throw PatternsNotSupported()

    override suspend fun getUsualActions(
        groupingKey: PatternGroupingKey,
        context: FacetSet,
        minimumConfidence: PatternConfidence?,
        maximumResults: Int,
    ): List<BehaviorPattern> = throw PatternsNotSupported()

    override suspend fun getPatternsAt(
        groupingKey: PatternGroupingKey,
        moment: OffsetDateTime?,
        alsoConstraining: FacetSet?,
        minimumConfidence: PatternConfidence?,
        maximumResults: Int,
    ): List<BehaviorPattern> = throw PatternsNotSupported()

    override suspend fun getPatternsForScope(groupingKey: PatternGroupingKey): List<BehaviorPattern> =
        throw PatternsNotSupported()

    override suspend fun getScopes(): List<PatternGroupingKey> = throw PatternsNotSupported()
}
