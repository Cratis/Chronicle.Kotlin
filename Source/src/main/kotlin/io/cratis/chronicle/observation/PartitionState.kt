// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

/** How a partition of a delivery settled. */
internal enum class PartitionState {
    /** Every event of the partition was observed. */
    Success,

    /** The partition failed at an event, and is recovered as a failed partition. */
    Failed,

    /** The partition is delivered again from an event after a delay, without failing. */
    Deferred,
}
