// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.concepts

/**
 * Marks a concept as the strongly-typed identity of an event source.
 *
 * The wire representation of an event source id is always a `String` - the kernel has no notion
 * of a typed identifier - which is why this is a `String`-backed [ConceptAs] rather than a generic
 * one. Implementing it is purely a declaration of intent: it does not change how the concept
 * serializes, and every existing overload that accepts a `ConceptAs<String>` as an event source id
 * (see `io.cratis.chronicle.concepts.EventSourceIdConcepts`) already works with it without change.
 *
 * The declaration matters for one thing: it is what [io.cratis.chronicle.compliance.Pii] checks
 * for. Marking an event source id `@Pii` would encrypt the very value the kernel uses to look up
 * the encryption key, making that key permanently unfindable - so the schema generator rejects it
 * with [io.cratis.chronicle.compliance.PiiNotSupportedOnEventSourceId]. If the identifier itself is
 * personal, use a random surrogate id as the event source id and keep the personal value in a
 * separate `@Pii` property instead.
 *
 * ```kotlin
 * data class EmployeeId(override val value: String) : EventSourceId
 * ```
 */
interface EventSourceId : ConceptAs<String>
