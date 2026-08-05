// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.readModels

/**
 * Represents the kind of change that occurred to a read model instance, as observed through
 * [IReadModelsService.watch].
 */
enum class ReadModelChangeType {
    /** The read model instance was created. */
    Added,

    /** The read model instance was updated. */
    Modified,

    /** The read model instance was removed. */
    Removed
}
