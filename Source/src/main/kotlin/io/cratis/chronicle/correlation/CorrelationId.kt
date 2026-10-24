// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.correlation

/**
 * Compatibility name for [io.cratis.fundamentals.execution.CorrelationId].
 * See the migration guide at `Documentation/guides/migrate-to-fundamentals.md`.
 */
@Deprecated("Moved to io.cratis.fundamentals.execution.CorrelationId.", ReplaceWith("CorrelationId", "io.cratis.fundamentals.execution.CorrelationId"))
typealias CorrelationId = io.cratis.fundamentals.execution.CorrelationId
