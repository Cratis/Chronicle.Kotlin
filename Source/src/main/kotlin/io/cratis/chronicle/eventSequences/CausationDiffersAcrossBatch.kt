// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

/**
 * Thrown when the events in one [IEventSequence.appendMany] batch declare different causation chains.
 *
 * A batch is committed as a single atomic operation and the kernel carries one causation chain for
 * it, not one per event. Two events in the same batch claiming different causes is therefore not
 * something the wire can express. Saying so is better than picking one of them, which would
 * attribute the rest of the batch to a cause that is not theirs.
 *
 * Split the batch, or leave causation unset and let the ambient chain cover all of it.
 */
class CausationDiffersAcrossBatch(eventCount: Int) : IllegalArgumentException(
    "The $eventCount events in this batch declare different causation chains. A batch is appended " +
        "under one chain, so either give them all the same causation, leave it unset to use the " +
        "ambient chain, or append them as separate batches."
)
