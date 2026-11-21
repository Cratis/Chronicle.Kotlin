// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.patterns

import Cratis.Chronicle.Contracts.Patterns.PatternsGrpcKt
import Cratis.Chronicle.Contracts.Patterns.PatternsOuterClass
import java.time.OffsetDateTime
import java.util.Locale

/**
 * Implements [IPatterns] by talking to the kernel over gRPC.
 *
 * @param eventStoreName The event store the patterns belong to.
 * @param namespace The namespace within the event store.
 * @param stub The stub patterns are queried through.
 */
class Patterns(
    private val eventStoreName: String,
    private val namespace: String,
    private val stub: PatternsGrpcKt.PatternsCoroutineStub,
) : IPatterns {

    override suspend fun getPatterns(
        groupingKey: PatternGroupingKey,
        context: FacetSet,
        minimumConfidence: PatternConfidence?,
        maximumResults: Int,
    ): List<BehaviorPattern> {
        val request = PatternsOuterClass.MatchingPatternsRequest.newBuilder()
            .setEventStore(eventStoreName)
            .setNamespace(namespace)
            .setGroupingKey(groupingKey.value)
            .putAllContext(context.toContract())
            // Zero means "whatever the server is configured for" on both of these; a default duplicated here
            // would silently disagree with the server's the moment either changed.
            .setMinimumConfidence(minimumConfidence?.value ?: 0.0)
            .setMaximumResults(maximumResults)
            .build()

        return stub.matchingPatterns(request).ensureSuccess("get matching patterns")
    }

    override suspend fun getUsualActions(
        groupingKey: PatternGroupingKey,
        context: FacetSet,
        minimumConfidence: PatternConfidence?,
        maximumResults: Int,
    ): List<BehaviorPattern> {
        val request = PatternsOuterClass.UsualActionsRequest.newBuilder()
            .setEventStore(eventStoreName)
            .setNamespace(namespace)
            .setGroupingKey(groupingKey.value)
            .putAllContext(context.toContract())
            .setMinimumConfidence(minimumConfidence?.value ?: 0.0)
            .setMaximumResults(maximumResults)
            .build()

        return stub.usualActions(request).ensureSuccess("get usual actions")
    }

    override suspend fun getPatternsAt(
        groupingKey: PatternGroupingKey,
        moment: OffsetDateTime?,
        alsoConstraining: FacetSet?,
        minimumConfidence: PatternConfidence?,
        maximumResults: Int,
    ): List<BehaviorPattern> {
        val at = moment ?: OffsetDateTime.now()

        // Built on whatever the caller already constrained, so asking about a moment and asking about the kind
        // of work are the same question. with() replaces, so a caller who constrained the day gets the moment's.
        val context = (alsoConstraining ?: FacetSet.empty)
            .with(FacetName.Day, at.dayOfWeek.name.lowercase(Locale.ROOT).replaceFirstChar { it.titlecase(Locale.ROOT) })
            .with(FacetName.TimeBucket, at.toTimeBucket().name)

        return getUsualActions(groupingKey, context, minimumConfidence, maximumResults)
    }

    override suspend fun getPatternsForScope(groupingKey: PatternGroupingKey): List<BehaviorPattern> {
        val request = PatternsOuterClass.PatternsForScopeRequest.newBuilder()
            .setEventStore(eventStoreName)
            .setNamespace(namespace)
            .setGroupingKey(groupingKey.value)
            .build()

        return stub.patternsForScope(request).ensureSuccess("get patterns for scope")
    }

    override suspend fun getScopes(): List<PatternGroupingKey> {
        val request = PatternsOuterClass.AllPatternScopesRequest.newBuilder()
            .setEventStore(eventStoreName)
            .setNamespace(namespace)
            .build()

        return stub.allPatternScopes(request).ensureSuccess("get pattern scopes")
    }

    private fun FacetSet.toContract(): Map<String, String> = facets.associate { it.name.value to it.value.value }
}
