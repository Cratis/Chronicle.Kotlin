// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.identity

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class IdentityTests {

    @Test
    fun `system identity has expected subject`() {
        assertEquals("5d032c92-9d5e-41eb-947a-ee5314ed0032", Identity.system.subject)
    }

    @Test
    fun `notSet identity has expected subject`() {
        assertEquals("1efc9b81-0612-4466-962c-86acc4e9a028", Identity.notSet.subject)
    }

    @Test
    fun `unknown identity has expected subject`() {
        assertEquals("3321cf62-db16-425e-8173-99fcfefe11dd", Identity.unknown.subject)
    }

    @Test
    fun `withoutDuplicates removes chain duplicates keeping first occurrence`() {
        val base = Identity("id1", "Alice", "alice")
        val middle = Identity("id2", "Bob", "bob", onBehalfOf = base)
        val top = Identity("id1", "Alice", "alice", onBehalfOf = middle)

        val deduped = top.withoutDuplicates()
        assertEquals("id1", deduped.subject)
        assertEquals("id2", deduped.onBehalfOf?.subject)
        assertNull(deduped.onBehalfOf?.onBehalfOf)
    }

    @Test
    fun `withoutDuplicates on single identity returns self-equivalent`() {
        val id = Identity("id1", "Alice", "alice")
        val result = id.withoutDuplicates()
        assertEquals(id.subject, result.subject)
        assertNull(result.onBehalfOf)
    }
}
