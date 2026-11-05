// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

/**
 * Reported when a batch method fails a partition without saying why.
 */
class PartitionFailedInBatch(partition: String) : Exception(
    "Partition '$partition' was failed by a batch method."
)
