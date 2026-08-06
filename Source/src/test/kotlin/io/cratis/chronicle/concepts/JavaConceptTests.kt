// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.concepts

import io.cratis.chronicle.json.chronicleGson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Java is a first-class caller, so a concept has to be something Java can declare and use. This is
 * why the client asks for an interface rather than a `@JvmInline value class`, whose mangled
 * signatures Java cannot work with.
 */
class JavaConceptTests {

    @Test
    fun `a java declared concept serializes as the value it wraps`() {
        val event = JavaConceptUsage.borrow("dune", "ada")
        assertEquals("""{"book":"dune","member":"ada"}""", chronicleGson.toJson(event))
    }

    @Test
    fun `a java declared concept reads back`() {
        val event = chronicleGson.fromJson(
            """{"book":"dune","member":"ada"}""",
            JavaConceptUsage.JavaBookBorrowed::class.java
        )

        assertEquals(JavaConceptUsage.JavaBookId("dune"), event.book())
        assertEquals(JavaConceptUsage.JavaMemberId("ada"), event.member())
    }

    @Test
    fun `java reads the underlying value through the interface`() {
        assertEquals("dune", JavaConceptUsage.valueOf(JavaConceptUsage.JavaBookId("dune")))
    }

    @Test
    fun `a kotlin declared concept is usable from java too`() {
        assertEquals("dune", JavaConceptUsage.valueOf(BookId("dune")))
    }
}
