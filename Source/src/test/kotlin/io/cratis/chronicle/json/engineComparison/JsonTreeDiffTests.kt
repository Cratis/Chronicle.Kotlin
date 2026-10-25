// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.json.engineComparison

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class JsonTreeDiffTests {
    @Test
    fun `numbers compare lexically and strings have a distinct type`() {
        assertEquals(listOf("$.n: value 1 -> 1.0", "$.s: type 1 -> \"1\""), jsonTreeDiff("""{"n":1,"s":1}""", """{"n":1.0,"s":"1"}"""))
    }

    @Test
    fun `object order is reported once without hiding missing and extra members`() {
        assertEquals(
            listOf("$.c: missing null -> <absent>", "$.d: extra <absent> -> true", "$: order [a, b] -> [b, a]"),
            jsonTreeDiff("""{"a":1,"b":2,"c":null}""", """{"b":2,"a":1,"d":true}""")
        )
    }

    @Test
    fun `array positions and nested member paths are stable and sorted`() {
        assertEquals(listOf("$[0].x: value false -> true", "$[1]: missing 2 -> <absent>"), jsonTreeDiff("""[{"x":false},2]""", """[{"x":true}]"""))
        assertEquals(listOf("$[0]: extra <absent> -> 1"), jsonTreeDiff("[]", "[1]"))
        assertEquals(emptyList<String>(), jsonTreeDiff("null", "null"))
    }
}
