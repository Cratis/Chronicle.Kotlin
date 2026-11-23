// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import Cratis.Chronicle.Contracts.Projections.ProjectionsGrpcKt
import Cratis.Chronicle.Contracts.Projections.ProjectionsOuterClass
import com.google.protobuf.Empty
import io.cratis.chronicle.readModels.ReadModelsService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** What a projection registration says about who is registering, and whether it waits for the kernel to apply it. */
class ProjectionRegistrationStatusTests {
    private val stub = mockk<ProjectionsGrpcKt.ProjectionsCoroutineStub>()
    private val request = slot<ProjectionsOuterClass.RegisterRequest>()

    init {
        coEvery { stub.register(capture(request), any()) } returns Empty.getDefaultInstance()
    }

    private fun service() = ProjectionsService("my-store", stub, mockk<ReadModelsService>(relaxed = true), "default")

    @Test
    fun `a registration made without a registrant sends none and does not ask for the status`() {
        runBlocking { service().register(SensorReading::class) }

        assertEquals("", request.captured.registrant)
        coVerify(exactly = 0) { stub.getRegistrationStatus(any(), any()) }
    }

    @Test
    fun `a registration made without a registrant does not claim to be the full set`() {
        runBlocking { service().register(SensorReading::class) }

        assertEquals(false, request.captured.fullSet)
    }
}
