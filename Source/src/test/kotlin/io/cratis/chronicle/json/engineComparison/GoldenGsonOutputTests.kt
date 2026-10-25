// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.json.engineComparison

import io.cratis.chronicle.json.chronicleGson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

class GoldenGsonOutputTests {
    @TestFactory
    fun `unmodified Gson reproduces every golden payload byte for byte`(): List<DynamicTest> = ComparisonFixtures.all.map { fixture ->
        DynamicTest.dynamicTest(fixture.name) {
            assertEquals(ComparisonFixtures.resource("golden", fixture.name), chronicleGson.toJson(fixture.sample))
        }
    }
}
