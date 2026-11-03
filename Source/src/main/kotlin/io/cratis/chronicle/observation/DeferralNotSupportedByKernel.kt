// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

/**
 * Reported when a batch method defers a partition but the kernel delivered the events without a
 * page, so it does not accept deferrals. The partition is failed instead, with this as the reason.
 */
class DeferralNotSupportedByKernel(partition: String) : Exception(
    "Partition '$partition' was deferred, but the kernel does not accept deferred partitions. " +
        "The partition is failed instead."
)
