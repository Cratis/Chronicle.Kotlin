// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.concepts

/**
 * Compatibility name for [io.cratis.fundamentals.concepts.ConceptAs].
 * See the migration guide at `Documentation/guides/migrate-to-fundamentals.md`.
 */
@Deprecated("Moved to io.cratis.fundamentals.concepts.ConceptAs; implement `fun value(): T`.", ReplaceWith("ConceptAs<T>", "io.cratis.fundamentals.concepts.ConceptAs"))
typealias ConceptAs<T> = io.cratis.fundamentals.concepts.ConceptAs<T>
