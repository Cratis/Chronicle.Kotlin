// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

/**
 * Represents an error that occurred during an append operation.
 *
 * @property message Human-readable description of the error.
 */
data class AppendError(val message: String)
