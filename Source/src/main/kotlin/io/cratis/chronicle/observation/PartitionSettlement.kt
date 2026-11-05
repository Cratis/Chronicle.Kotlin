// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

/**
 * Settles the outcome of a partition from the problems the methods of an observer reported for it.
 *
 * A partition succeeds when no method reported a problem for it. Otherwise it settles at its earliest
 * problem: every event before it counts as observed, and everything from it is delivered again -
 * including events another method of the observer already handled.
 */
internal object PartitionSettlement {
    /**
     * Settles [partition], whose events in the delivery carry [sequenceNumbers] in order, from the
     * [problems] reported for any partition.
     */
    fun settle(partition: String, sequenceNumbers: List<Long>, problems: List<PartitionProblem>): PartitionOutcome {
        val earliest = problems.filter { it.partition == partition }.minByOrNull { it.at }
            ?: return PartitionOutcome(
                partition,
                PartitionState.Success,
                sequenceNumbers.lastOrNull() ?: PartitionOutcome.NONE_OBSERVED,
            )

        return PartitionOutcome(
            partition,
            earliest.state,
            sequenceNumbers.lastOrNull { it < earliest.at } ?: PartitionOutcome.NONE_OBSERVED,
            earliest.messages,
            earliest.stackTrace,
            earliest.retryAfter,
        )
    }
}
