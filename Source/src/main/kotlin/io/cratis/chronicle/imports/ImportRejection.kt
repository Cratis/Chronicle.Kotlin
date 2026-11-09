// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.imports

/**
 * An event the kernel did not accept, and why.
 *
 * @property index The position of the event in the import, counted from zero across every chunk.
 * @property eventType The identifier of the event's type.
 * @property reasons Why it was rejected.
 */
data class ImportRejection(val index: Long, val eventType: String, val reasons: List<String>)
