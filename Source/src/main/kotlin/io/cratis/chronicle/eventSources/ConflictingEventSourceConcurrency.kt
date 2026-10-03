// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSources

/**
 * Thrown when one atomic batch holds events for the same event source identifier whose event source
 * definitions derive different concurrency scopes.
 *
 * The kernel takes a single concurrency scope per event source identifier in a batch, so it cannot guard
 * each event by its own definition. Applying one of them would silently leave the other event unguarded or
 * guarded by the wrong dimensions, so the batch is rejected before anything is appended. Supply an explicit
 * scope for the identifier, or append the events in separate batches.
 */
class ConflictingEventSourceConcurrency(val eventSourceId: String) : RuntimeException(
    "Events for event source id '$eventSourceId' go through event source definitions that derive different " +
        "concurrency scopes, and a batch carries one scope per event source id. Supply an explicit " +
        "concurrency scope for it, or append them in separate batches."
)
