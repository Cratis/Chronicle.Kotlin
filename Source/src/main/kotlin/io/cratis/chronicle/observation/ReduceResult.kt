// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import java.time.Duration

/** What folding the events of one event source through a reducer came to. */
internal sealed interface ReduceResult {
    /**
     * Every event was folded.
     *
     * @property state The read model the events produced, or `null` when the reducer removed it.
     * @property lastObserved The sequence number of the last event folded.
     */
    data class Reduced(val state: Any?, val lastObserved: Long) : ReduceResult

    /**
     * The fold failed. The reducer writes no state for the event source, so the whole delivery of it
     * is folded again once the failed partition is recovered.
     *
     * @property error What failed it.
     * @property methodName The method that failed, for the message when [error] carries none.
     */
    data class Failed(val error: Throwable, val methodName: String) : ReduceResult

    /**
     * The batch method deferred the event source: it is delivered again after [retryAfter].
     *
     * @property retryAfter How long to wait before delivering it again.
     */
    data class Deferred(val retryAfter: Duration) : ReduceResult
}
