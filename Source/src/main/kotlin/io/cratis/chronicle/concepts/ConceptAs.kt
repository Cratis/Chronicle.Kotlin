// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.concepts

/**
 * A domain value wrapped in a type of its own, so the compiler can tell one identifier from another.
 *
 * A `String` is a `String` is a `String`. Nothing stops a book's identifier being passed where a
 * member's was expected - both compile, and the bug shows up in production as a lookup that finds
 * nothing. Give each value its own type and the compiler catches it while you are still typing:
 *
 * ```kotlin
 * data class BookId(override val value: String) : ConceptAs<String>
 * data class MemberId(override val value: String) : ConceptAs<String>
 *
 * @EventType
 * data class BookBorrowed(val book: BookId, val member: MemberId)
 *
 * // Won't compile - and that is the entire point.
 * BookBorrowed(memberId, bookId)
 * ```
 *
 * A concept serializes as its underlying value, not as an object wrapping one, so
 * `BookBorrowed(BookId("dune"), MemberId("ada"))` goes on the wire as
 * `{"book":"dune","member":"ada"}` and the kernel sees a plain string. That is what lets a concept be
 * introduced into an event that is already in production without rewriting a single stored event, and
 * what makes the schema the kernel validates against unchanged.
 *
 * Declare concepts as `data class` rather than `@JvmInline value class`. Both work, and a value class
 * avoids the allocation, but its mangled JVM signatures are painful from Java - and Java is a
 * first-class caller here. Use a value class only where the allocation genuinely matters and Java
 * never touches the type.
 *
 * Anywhere the client takes an event source id as a `String`, there is an overload taking a
 * `ConceptAs<String>` - see the extensions in `io.cratis.chronicle.concepts`.
 *
 * @param T The underlying value type. Anything the client can already serialize: `String`, a number,
 *   a `UUID`, an `Instant`.
 */
interface ConceptAs<T : Any> {
    /** The underlying value. */
    val value: T
}
