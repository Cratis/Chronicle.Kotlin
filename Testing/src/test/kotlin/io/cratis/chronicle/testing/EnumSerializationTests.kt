// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.testing

import io.cratis.chronicle.events.EventType
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

private enum class ParcelStatus { Pending, Dispatched }

@EventType
private data class ParcelStatusChanged(val status: ParcelStatus)

class EnumSerializationTests {
    @Test
    fun `an in-memory append stores enum numbers and the scenario reads the typed event back`() = runBlocking {
        val scenario = EventScenario()
        val event = ParcelStatusChanged(ParcelStatus.Dispatched)

        scenario.eventLog.append("parcel-1", event)

        assertEquals("""{"status":1}""", scenario.eventLog.events.single().content)
        assertEquals(event, scenario.shouldHaveAppended<ParcelStatusChanged>("parcel-1"))
    }
}
