// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.patterns

import Cratis.Chronicle.Contracts.Patterns.PatternsOuterClass
import io.cratis.chronicle.eventSequences.ChronicleCommandRejected
import java.time.OffsetDateTime

/** The patterns of a successful query, or [ChronicleCommandRejected] when the kernel failed it. */
internal fun PatternsOuterClass.QueryResult_IEnumerable_BehaviorPatternDetailsResponse.ensureSuccess(
    operation: String,
): List<BehaviorPattern> {
    ensureSuccess(operation, isAuthorized, exceptionMessagesList, validationResultsList)
    return dataList.map { it.toClient() }
}

/** The scopes of a successful query, or [ChronicleCommandRejected] when the kernel failed it. */
internal fun PatternsOuterClass.QueryResult_IEnumerable_PatternScopeResponse.ensureSuccess(
    operation: String,
): List<PatternGroupingKey> {
    ensureSuccess(operation, isAuthorized, exceptionMessagesList, validationResultsList)
    return dataList.map { PatternGroupingKey(it.id) }
}

/** The client form of a pattern the kernel returned. */
internal fun PatternsOuterClass.BehaviorPatternDetailsResponse.toClient() = BehaviorPattern(
    groupingKey = PatternGroupingKey(groupingKey),
    facets = FacetSet(facetsMap.map { (name, value) -> Facet(FacetName(name), FacetValue(value)) }),
    occurrences = occurrences,
    confidence = PatternConfidence(confidence),
    support = support,
    weight = weight,
    firstSeen = if (hasFirstSeen()) firstSeen.toOffsetDateTime() else null,
    lastSeen = if (hasLastSeen()) lastSeen.toOffsetDateTime() else null,
)

private fun PatternsOuterClass.SerializableDateTimeOffset.toOffsetDateTime(): OffsetDateTime? =
    value.takeIf { it.isNotBlank() }?.let(OffsetDateTime::parse)

private fun ensureSuccess(
    operation: String,
    isAuthorized: Boolean,
    exceptionMessages: List<String>,
    validationResults: List<PatternsOuterClass.ValidationResult>,
) {
    if (!isAuthorized) throw ChronicleCommandRejected(operation, "not authorized")
    if (exceptionMessages.isNotEmpty()) throw ChronicleCommandRejected(operation, exceptionMessages.joinToString("; "))
    if (validationResults.isNotEmpty()) throw ChronicleCommandRejected(operation, validationResults.joinToString("; ") { it.message })
}
