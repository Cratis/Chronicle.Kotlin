// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.patterns

import java.time.OffsetDateTime

/**
 * Asks the kernel about the behavior patterns it has mined.
 *
 * A minimum confidence of `null` and a maximum of `0` results mean "whatever the server is configured for";
 * the client carries no thresholds of its own.
 */
interface IPatterns {
    /**
     * The patterns of [groupingKey] that hold in [context] - the low-level facet-set query.
     *
     * @param groupingKey The scope to ask about, normally the user.
     * @param context The facets the patterns must hold in.
     * @param minimumConfidence The least confidence a pattern must have, or `null` for the server's default.
     * @param maximumResults The most patterns to return, or `0` for the server's default.
     */
    suspend fun getPatterns(
        groupingKey: PatternGroupingKey,
        context: FacetSet,
        minimumConfidence: PatternConfidence? = null,
        maximumResults: Int = 0,
    ): List<BehaviorPattern>

    /**
     * The actions [groupingKey] usually takes in [context].
     *
     * @param groupingKey The scope to ask about, normally the user.
     * @param context The facets the actions must have been established in.
     * @param minimumConfidence The least confidence a pattern must have, or `null` for the server's default.
     * @param maximumResults The most patterns to return, or `0` for the server's default.
     */
    suspend fun getUsualActions(
        groupingKey: PatternGroupingKey,
        context: FacetSet,
        minimumConfidence: PatternConfidence? = null,
        maximumResults: Int = 0,
    ): List<BehaviorPattern>

    /**
     * What [groupingKey] normally does at [moment] - the actions usually taken on that day of the week in that
     * part of the day.
     *
     * The `Day` and `TimeBucket` facets are derived from [moment] in its own offset, with the same rule the
     * kernel mines with, and replace any value [alsoConstraining] has for them. Pass a moment carrying the
     * offset of the person asked about.
     *
     * @param groupingKey The scope to ask about, normally the user.
     * @param moment The moment to ask about, or `null` for now in the system's default offset.
     * @param alsoConstraining Further facets to narrow the question by, such as an aggregate type.
     * @param minimumConfidence The least confidence a pattern must have, or `null` for the server's default.
     * @param maximumResults The most patterns to return, or `0` for the server's default.
     */
    suspend fun getPatternsAt(
        groupingKey: PatternGroupingKey,
        moment: OffsetDateTime? = null,
        alsoConstraining: FacetSet? = null,
        minimumConfidence: PatternConfidence? = null,
        maximumResults: Int = 0,
    ): List<BehaviorPattern>

    /**
     * Every pattern established for [groupingKey].
     *
     * @param groupingKey The scope to ask about.
     */
    suspend fun getPatternsForScope(groupingKey: PatternGroupingKey): List<BehaviorPattern>

    /** Every scope the kernel has established patterns for. */
    suspend fun getScopes(): List<PatternGroupingKey>

    companion object {
        /** The patterns of an event store implementation that does not support them; every call throws [PatternsNotSupported]. */
        val unsupported: IPatterns = UnsupportedPatterns
    }
}
