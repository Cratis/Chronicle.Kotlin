// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.concepts

import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.json.chronicleGson
import io.cratis.chronicle.schemas.JsonSchemaGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.UUID

data class BookId(override val value: String) : ConceptAs<String>

data class MemberId(override val value: String) : ConceptAs<String>

data class CopyNumber(override val value: Int) : ConceptAs<Int>

data class LoanId(override val value: UUID) : ConceptAs<UUID>

@JvmInline
value class ShelfCode(override val value: String) : ConceptAs<String>

/** A concept reached through an interface of its own rather than implementing ConceptAs directly. */
interface Identifier : ConceptAs<String>

data class BranchId(override val value: String) : Identifier

@EventType
data class BookBorrowed(
    val book: BookId = BookId(""),
    val member: MemberId = MemberId(""),
    val copy: CopyNumber = CopyNumber(0),
    val shelf: ShelfCode = ShelfCode("")
)

/**
 * A concept gives a domain value its own type so the compiler can tell one identifier from another.
 * What has to hold on the wire: it serializes as the value it wraps, so adopting one for a property
 * that used to be a plain String changes neither the JSON nor the schema.
 */
class ConceptAsTests {

    @Test
    fun `a concept serializes as the value it wraps`() {
        assertEquals("\"dune\"", chronicleGson.toJson(BookId("dune")))
    }

    @Test
    fun `an event carrying concepts looks exactly like one carrying plain values`() {
        val json = chronicleGson.toJson(
            BookBorrowed(BookId("dune"), MemberId("ada"), CopyNumber(3), ShelfCode("A-14"))
        )

        assertEquals("""{"book":"dune","member":"ada","copy":3,"shelf":"A-14"}""", json)
    }

    @Test
    fun `a concept reads back from the value it was written as`() {
        val event = chronicleGson.fromJson(
            """{"book":"dune","member":"ada","copy":3,"shelf":"A-14"}""",
            BookBorrowed::class.java
        )

        assertEquals(BookId("dune"), event.book)
        assertEquals(MemberId("ada"), event.member)
        assertEquals(CopyNumber(3), event.copy)
        assertEquals(ShelfCode("A-14"), event.shelf)
    }

    @Test
    fun `a concept over a non-string value keeps that value's json type`() {
        assertEquals("3", chronicleGson.toJson(CopyNumber(3)))
        val loanId = UUID.fromString("6f1a9b6a-0f6d-4f3f-9b8e-9a2f0d5e1c77")
        assertEquals("\"$loanId\"", chronicleGson.toJson(LoanId(loanId)))
    }

    @Test
    fun `a concept declared through an interface of its own is still just its value`() {
        assertEquals("\"oslo\"", chronicleGson.toJson(BranchId("oslo")))
        assertEquals(BranchId("oslo"), chronicleGson.fromJson("\"oslo\"", BranchId::class.java))
    }

    @Test
    fun `a null concept stays null`() {
        assertEquals("null", chronicleGson.toJson(null, BookId::class.java))
        assertNull(chronicleGson.fromJson("null", BookId::class.java))
    }

    @Test
    fun `two concepts over the same value are still different values`() {
        // The compiler already refuses to mix them; this pins that they do not compare equal either,
        // which is what makes them safe to use as map keys and in assertions.
        assertFalse(BookId("dune").equals(MemberId("dune")))
    }

    @Test
    fun `the schema describes the value a concept wraps, not the wrapper`() {
        val schema = JsonSchemaGenerator.generate(BookBorrowed::class)

        assertEquals(
            """{"type":"object","properties":{"book":{"type":"string"},""" +
                """"copy":{"type":"integer","format":"int32"},""" +
                """"member":{"type":"string"},"shelf":{"type":"string"}}}""",
            schema
        )
    }
}
