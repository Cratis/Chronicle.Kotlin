// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.imports

/**
 * Thrown when the kernel refuses a chunk of an import outright, rather than rejecting single events in it.
 *
 * @property chunkIndex The index of the refused chunk.
 */
class ImportChunkRefused(val chunkIndex: Int, reason: String) : RuntimeException(
    "Chunk $chunkIndex of the import was refused: $reason"
)
