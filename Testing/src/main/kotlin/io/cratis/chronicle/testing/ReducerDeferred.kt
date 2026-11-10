// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.testing

import java.time.Duration

/**
 * Thrown by [ReadModelScenario.fold] when a batch reducer defers the event source being folded, so no
 * read model is written.
 *
 * Chronicle delivers the deferred events again after [retryAfter] and keeps the state it had before them
 * until then, which is what the scenario keeps too.
 *
 * @param reducerName The reducer that deferred.
 * @property eventSourceId The event source that was deferred.
 * @property retryAfter How long the reducer asked to wait before the events are delivered again.
 */
class ReducerDeferred(
    reducerName: String,
    val eventSourceId: String,
    val retryAfter: Duration,
) : Exception(
    "Reducer '$reducerName' deferred event source '$eventSourceId' for $retryAfter. Chronicle delivers its " +
        "events again after that delay, and writes no read model until then.",
)
