// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import io.cratis.chronicle.eventSequences.EventSequenceId

interface IProjectionsService {
    suspend fun register(vararg projections: Any)

    /**
     * Runs a Projection Declaration Language declaration and returns what it projected, without
     * registering anything.
     *
     * A registered projection is the right answer when a read model is part of the system: the
     * kernel keeps it up to date, and the client hands you a type. This is for the questions that
     * are not part of the system - a one-off question of the event log during an incident, a report
     * nobody wants to deploy a projection for, a declaration being written in an editor and tried
     * out as it is typed.
     *
     * Nothing is registered, nothing is persisted, and nothing observes afterwards. The kernel
     * projects over the sequence and hands back the result.
     *
     * ```kotlin
     * val result = store.projections.query(
     *     """
     *     from EmployeeHired
     *         set name to firstName
     *     """
     * )
     *
     * when (result) {
     *     is ProjectionQueryResult.Projected -> result.instancesOf(EmployeeName::class).forEach(::println)
     *     is ProjectionQueryResult.Invalid -> result.errors.forEach(::println)
     * }
     * ```
     *
     * @param declaration The declaration to run.
     * @param eventSequenceId The sequence to project over. Defaults to the event log.
     * @return What it projected, or what is wrong with the declaration.
     */
    suspend fun query(
        declaration: String,
        eventSequenceId: EventSequenceId = EventSequenceId.eventLog
    ): ProjectionQueryResult
}
