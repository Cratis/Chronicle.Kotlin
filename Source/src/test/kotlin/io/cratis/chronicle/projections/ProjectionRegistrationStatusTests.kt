// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import Cratis.Chronicle.Contracts.Projections.ProjectionsGrpcKt
import Cratis.Chronicle.Contracts.Projections.ProjectionsOuterClass
import com.google.protobuf.Empty
import io.cratis.chronicle.readModels.ReadModelsService
import io.grpc.Metadata
import io.grpc.Status
import io.grpc.StatusException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/** What a projection registration says about who is registering, and whether it waits for the kernel to apply it. */
class ProjectionRegistrationStatusTests {
    private val stub = mockk<ProjectionsGrpcKt.ProjectionsCoroutineStub>()
    private val request = slot<ProjectionsOuterClass.RegisterRequest>()

    init {
        coEvery { stub.register(capture(request), any()) } returns Empty.getDefaultInstance()
    }

    private val waits = mutableListOf<Duration>()

    private fun service() = ProjectionsService("my-store", stub, mockk<ReadModelsService>(relaxed = true), "default")

    private fun service(registrant: String) =
        ProjectionsService("my-store", stub, mockk<ReadModelsService>(relaxed = true), "default", registrant).also {
            it.waitForKernel = { wait -> waits += wait }
        }

    private fun status(state: ProjectionsOuterClass.RegistrationState, failure: String = "") =
        ProjectionsOuterClass.RegistrationStatus.newBuilder().setState(state).setFailure(failure).build()

    private val pending = status(ProjectionsOuterClass.RegistrationState.Pending)
    private val applied = status(ProjectionsOuterClass.RegistrationState.Applied)

    private fun busy(retryAfter: String?): StatusException {
        val trailers = Metadata()
        retryAfter?.let { trailers.put(Metadata.Key.of("retry-after", Metadata.ASCII_STRING_MARSHALLER), it) }
        return Status.UNAVAILABLE.withDescription("busy").asException(trailers)
    }

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

    @Test
    fun `a registration made as a registrant names it, without claiming to be the full set`() {
        coEvery { stub.getRegistrationStatus(any(), any()) } returns applied

        runBlocking { service("my-app").register(SensorReading::class) }

        assertEquals("my-app", request.captured.registrant)
        assertEquals(false, request.captured.fullSet)
    }

    @Test
    fun `it waits until the kernel has applied what it accepted`() {
        coEvery { stub.getRegistrationStatus(any(), any()) } returnsMany listOf(pending, pending, applied)

        runBlocking { service("my-app").register(SensorReading::class) }

        coVerify(exactly = 3) { stub.getRegistrationStatus(any(), any()) }
        assertEquals(listOf(50.milliseconds, 100.milliseconds), waits)
    }

    @Test
    fun `the wait between questions grows but stops at two seconds`() {
        coEvery { stub.getRegistrationStatus(any(), any()) } returnsMany List(8) { pending } + applied

        runBlocking { service("my-app").register(SensorReading::class) }

        assertEquals(
            listOf(50.milliseconds, 100.milliseconds, 200.milliseconds, 400.milliseconds, 800.milliseconds, 1600.milliseconds, 2.seconds, 2.seconds),
            waits
        )
    }

    @Test
    fun `a registration the kernel could not apply fails with why`() {
        coEvery { stub.getRegistrationStatus(any(), any()) } returns
            status(ProjectionsOuterClass.RegistrationState.Failed, "the read model does not exist")

        val failure = assertThrows(ProjectionRegistrationNotApplied::class.java) {
            runBlocking { service("my-app").register(SensorReading::class) }
        }

        assertEquals("my-app", failure.registrant)
        assertEquals("the read model does not exist", failure.failure)
    }

    @Test
    fun `a kernel that has no registration status is not waited for`() {
        coEvery { stub.getRegistrationStatus(any(), any()) } throws Status.UNIMPLEMENTED.asException()

        runBlocking { service("my-app").register(SensorReading::class) }

        coVerify(exactly = 1) { stub.getRegistrationStatus(any(), any()) }
        assertEquals(emptyList<Duration>(), waits)
    }

    @Test
    fun `any other failure asking for the status is not swallowed`() {
        coEvery { stub.getRegistrationStatus(any(), any()) } throws Status.INTERNAL.asException()

        assertThrows(StatusException::class.java) { runBlocking { service("my-app").register(SensorReading::class) } }
    }

    @Test
    fun `it stops waiting for a registration that is still being applied after the longest wait`() {
        coEvery { stub.getRegistrationStatus(any(), any()) } returns pending

        runBlocking {
            service("my-app").also { it.maxWaitForRegistration = Duration.ZERO }.register(SensorReading::class)
        }

        coVerify(exactly = 1) { stub.getRegistrationStatus(any(), any()) }
    }

    @Test
    fun `a busy kernel is asked again after the wait it named`() {
        var calls = 0
        coEvery { stub.register(any(), any()) } answers { if (calls++ == 0) throw busy("3") else Empty.getDefaultInstance() }
        coEvery { stub.getRegistrationStatus(any(), any()) } returns applied

        runBlocking { service("my-app").register(SensorReading::class) }

        assertEquals(2, calls)
        assertEquals(listOf(3.seconds), waits)
    }

    @Test
    fun `a busy kernel is waited for even when the program did not name itself`() {
        var calls = 0
        coEvery { stub.register(any(), any()) } answers { if (calls++ == 0) throw busy("1") else Empty.getDefaultInstance() }

        runBlocking { service("").register(SensorReading::class) }

        assertEquals(2, calls)
        assertEquals(listOf(1.seconds), waits)
    }

    @Test
    fun `an unavailable kernel that names no wait fails the registration`() {
        coEvery { stub.register(any(), any()) } throws busy(null)

        assertThrows(StatusException::class.java) { runBlocking { service("my-app").register(SensorReading::class) } }
        assertEquals(emptyList<Duration>(), waits)
    }

    @Test
    fun `a kernel that stays busy past the longest wait fails the registration`() {
        coEvery { stub.register(any(), any()) } throws busy("30")

        assertThrows(StatusException::class.java) {
            runBlocking { service("my-app").also { it.maxWaitForRegistration = 20.seconds }.register(SensorReading::class) }
        }
        assertEquals(emptyList<Duration>(), waits)
    }
}
