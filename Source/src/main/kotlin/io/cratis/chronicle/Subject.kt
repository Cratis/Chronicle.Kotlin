// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle

/**
 * Marks a property as the compliance subject — the identity PII is protected against, and the
 * encryption key a release is performed under.
 *
 * [io.cratis.chronicle.readModels.IReadModelsService.release] uses this to pick which key a read
 * model instance is released against. Without an explicit subject and without this annotation
 * anywhere on the instance, release falls back to a property named `id` (case-insensitive) - the
 * convention every read model has followed so far. A read model whose subject genuinely is a
 * different property must mark it, or a release silently uses the wrong key and leaves the actual
 * subject's PII unprotected.
 *
 * ```kotlin
 * @ReadModel
 * data class CustomerProfile(
 *     val id: String = "",
 *     @Subject val customerId: String = "",
 *     val email: String = ""
 * )
 * ```
 *
 * This mirrors .NET's `Cratis.Chronicle.SubjectAttribute`. The .NET client also applies it to event
 * properties, to derive the append subject when the caller supplies none - that half is
 * `io.cratis.chronicle.events` territory and is not covered here.
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Subject
