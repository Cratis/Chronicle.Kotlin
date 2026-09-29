// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import Cratis.Chronicle.Contracts.Projections.ProjectionsGrpcKt
import Cratis.Chronicle.Contracts.Projections.ProjectionsOuterClass
import com.google.gson.JsonParser
import com.google.protobuf.Empty
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.readModels.ReadModel
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@EventType
class ReadingRecorded

// Public on purpose: the client builds the initial model state by calling the read model's
// constructor reflectively, and a file-private class is not accessible to it.
@ReadModel
@FromEvent(ReadingRecorded::class)
data class SensorReading(
    val id: String = "",
    val recordedAt: Instant = Instant.parse("2024-05-01T10:15:30Z"),
    val offsetAt: OffsetDateTime = OffsetDateTime.of(2024, 5, 1, 10, 15, 30, 0, ZoneOffset.UTC),
    val zonedAt: ZonedDateTime = ZonedDateTime.of(2024, 5, 1, 10, 15, 30, 0, ZoneOffset.UTC),
    val localAt: LocalDateTime = LocalDateTime.of(2024, 5, 1, 10, 15, 30),
    val day: LocalDate = LocalDate.of(2024, 5, 1),
    val time: LocalTime = LocalTime.of(10, 15, 30)
)

class ProjectionInitialModelStateTests {

    private fun initialModelStateOf(cls: Any): String {
        val stub = mockk<ProjectionsGrpcKt.ProjectionsCoroutineStub>()
        val request = slot<ProjectionsOuterClass.RegisterRequest>()
        coEvery { stub.register(capture(request), any()) } returns Empty.getDefaultInstance()
        val service = ProjectionsService(
            "my-store",
            stub,
            mockk<io.cratis.chronicle.readModels.ReadModelsService>(relaxed = true),
            "default"
        )
        runBlocking { service.register(cls) }
        return request.captured.projectionsList.single().initialModelState
    }

    @Test
    fun `an instant default is an ISO string in the initial model state`() {
        val state = JsonParser.parseString(initialModelStateOf(SensorReading::class)).asJsonObject
        assertTrue(state.get("recordedAt").isJsonPrimitive)
        assertEquals("2024-05-01T10:15:30Z", state.get("recordedAt").asString)
    }

    @Test
    fun `every java time default is a string in the initial model state`() {
        val state = JsonParser.parseString(initialModelStateOf(SensorReading::class)).asJsonObject
        assertEquals("2024-05-01T10:15:30Z", state.get("offsetAt").asString)
        assertEquals("2024-05-01T10:15:30Z", state.get("zonedAt").asString)
        assertEquals("2024-05-01T10:15:30", state.get("localAt").asString)
        assertEquals("2024-05-01", state.get("day").asString)
        assertEquals("10:15:30", state.get("time").asString)
    }
}
