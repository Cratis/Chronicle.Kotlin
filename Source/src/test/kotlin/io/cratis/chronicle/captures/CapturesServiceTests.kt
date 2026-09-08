// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.captures

import Cratis.Chronicle.Contracts.Captures.CapturesGrpcKt
import Cratis.Chronicle.Contracts.Captures.CapturesOuterClass
import bcl.Bcl
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.util.UUID
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * A capture pulls something that is not Chronicle and appends what it finds as events. It is defined
 * by a declaration, so most of what can go wrong is the kernel not making sense of the text - which
 * is an outcome, not an exception.
 */
class CapturesServiceTests {

    private val declaration = "capture ExchangeRates\n    from api \"https://example.com/rates\""

    // The kernel takes capture ids as a wire Guid, so calls into the service need a real UUID - the
    // "exchange-rates" string below is only ever the display id the kernel hands back in a response.
    private val captureId = "3fa85f64-5717-4562-b3fc-2c963f66afa6"

    private fun String.toContractGuid(): Bcl.Guid {
        val uuid = UUID.fromString(this)
        return Bcl.Guid.newBuilder()
            .setLo(java.lang.Long.reverseBytes(uuid.mostSignificantBits))
            .setHi(java.lang.Long.reverseBytes(uuid.leastSignificantBits))
            .build()
    }

    private fun capture(status: CapturesOuterClass.CaptureStatus) =
        CapturesOuterClass.CaptureDetailsResponse.newBuilder()
            .setId("exchange-rates")
            .setName("ExchangeRates")
            .setDeclaration(declaration)
            .setStatus(status)
            .build()

    private fun message(text: String, line: Int, column: Int) =
        CapturesOuterClass.CaptureValidationMessage.newBuilder()
            .setMessage(text)
            .setLine(line)
            .setColumn(column)
            .build()

    private fun serviceFor(stub: CapturesGrpcKt.CapturesCoroutineStub) = CapturesService("my-store", stub)

    @Test
    fun `every capture the store holds comes back`() = runBlocking {
        val stub = mockk<CapturesGrpcKt.CapturesCoroutineStub>()
        coEvery { stub.getCaptures(any(), any()) } returns
            CapturesOuterClass.QueryResult_IEnumerable_CaptureDetailsResponse.newBuilder()
                .addData(capture(CapturesOuterClass.CaptureStatus.Started))
                .build()

        val captures = serviceFor(stub).getAll()

        assertEquals("exchange-rates", captures.single().id)
        assertEquals("ExchangeRates", captures.single().name)
        assertEquals(declaration, captures.single().declaration)
        assertEquals(CaptureStatus.Started, captures.single().status)
        assertTrue(captures.single().isStarted)
    }

    @Test
    fun `a stopped capture reads as stopped`() = runBlocking {
        val stub = mockk<CapturesGrpcKt.CapturesCoroutineStub>()
        coEvery { stub.getCaptures(any(), any()) } returns
            CapturesOuterClass.QueryResult_IEnumerable_CaptureDetailsResponse.newBuilder()
                .addData(capture(CapturesOuterClass.CaptureStatus.Stopped))
                .build()

        assertFalse(serviceFor(stub).getAll().single().isStarted)
    }

    @Test
    fun `observing re-emits the whole set on every change`() = runBlocking {
        val stub = mockk<CapturesGrpcKt.CapturesCoroutineStub>()
        every { stub.observeCaptures(any(), any()) } returns flowOf(
            CapturesOuterClass.QueryResult_IEnumerable_CaptureDetailsResponse.newBuilder()
                .addData(capture(CapturesOuterClass.CaptureStatus.Stopped))
                .build(),
            CapturesOuterClass.QueryResult_IEnumerable_CaptureDetailsResponse.newBuilder()
                .addData(capture(CapturesOuterClass.CaptureStatus.Started))
                .build()
        )

        val emitted = serviceFor(stub).observeAll().toList()

        assertEquals(listOf(CaptureStatus.Stopped, CaptureStatus.Started), emitted.map { it.single().status })
    }

    @Test
    fun `saving a declaration the kernel accepts returns the capture it now holds`() = runBlocking {
        val stub = mockk<CapturesGrpcKt.CapturesCoroutineStub>()
        coEvery { stub.saveCapture(any(), any()) } returns
            CapturesOuterClass.CommandResult_SaveCaptureResponse.newBuilder()
                .setIsAuthorized(true)
                .setResponse(
                    CapturesOuterClass.SaveCaptureResponse.newBuilder()
                        .setCapture(capture(CapturesOuterClass.CaptureStatus.Stopped))
                        .build()
                )
                .build()

        val result = serviceFor(stub).save(captureId, declaration)

        val accepted = assertInstanceOf(CaptureDeclarationResult.Accepted::class.java, result)
        assertTrue(accepted.isSuccess)
        assertEquals("exchange-rates", accepted.capture.id)
        assertTrue(accepted.messages.isEmpty())
    }

    @Test
    fun `a declaration the kernel rejects comes back as messages rather than throwing`() = runBlocking {
        val stub = mockk<CapturesGrpcKt.CapturesCoroutineStub>()
        coEvery { stub.saveCapture(any(), any()) } returns
            CapturesOuterClass.CommandResult_SaveCaptureResponse.newBuilder()
                .setIsAuthorized(true)
                .setResponse(
                    CapturesOuterClass.SaveCaptureResponse.newBuilder()
                        .addMessages(message("unknown source kind 'apo'", 2, 10))
                        .build()
                )
                .build()

        val result = serviceFor(stub).save(captureId, declaration)

        val rejected = assertInstanceOf(CaptureDeclarationResult.Rejected::class.java, result)
        assertFalse(rejected.isSuccess)
        assertEquals("2:10: unknown source kind 'apo'", rejected.messages.single().toString())
    }

    @Test
    fun `a declaration accepted with something worth saying keeps both`() = runBlocking {
        // Messages alone do not mean rejection - the capture coming back is what says it took.
        val stub = mockk<CapturesGrpcKt.CapturesCoroutineStub>()
        coEvery { stub.saveCapture(any(), any()) } returns
            CapturesOuterClass.CommandResult_SaveCaptureResponse.newBuilder()
                .setIsAuthorized(true)
                .setResponse(
                    CapturesOuterClass.SaveCaptureResponse.newBuilder()
                        .setCapture(capture(CapturesOuterClass.CaptureStatus.Stopped))
                        .addMessages(message("polling faster than the source updates", 2, 30))
                        .build()
                )
                .build()

        val result = serviceFor(stub).save(captureId, declaration)

        assertTrue(result.isSuccess)
        assertEquals(1, result.messages.size)
    }

    @Test
    fun `validating checks a declaration without saving it`() = runBlocking {
        val stub = mockk<CapturesGrpcKt.CapturesCoroutineStub>()
        val request = slot<CapturesOuterClass.ValidateCaptureDeclarationRequest>()
        coEvery { stub.validateCaptureDeclaration(capture(request), any()) } returns
            CapturesOuterClass.CommandResult_ValidateCaptureDeclarationResponse.newBuilder()
                .setIsAuthorized(true)
                .setResponse(
                    CapturesOuterClass.ValidateCaptureDeclarationResponse.newBuilder()
                        .addMessages(message("unknown source kind 'apo'", 2, 10))
                        .build()
                )
                .build()

        val messages = serviceFor(stub).validate(declaration)

        assertEquals(declaration, request.captured.declaration)
        assertEquals("my-store", request.captured.eventStore)
        assertEquals(1, messages.size)
    }

    @Test
    fun `a declaration the kernel is happy with validates to nothing at all`() = runBlocking {
        val stub = mockk<CapturesGrpcKt.CapturesCoroutineStub>()
        coEvery { stub.validateCaptureDeclaration(any(), any()) } returns
            CapturesOuterClass.CommandResult_ValidateCaptureDeclarationResponse.newBuilder()
                .setIsAuthorized(true)
                .setResponse(CapturesOuterClass.ValidateCaptureDeclarationResponse.newBuilder().build())
                .build()

        assertTrue(serviceFor(stub).validate(declaration).isEmpty())
    }

    @Test
    fun `starting a capture that cannot start says why`() = runBlocking {
        val stub = mockk<CapturesGrpcKt.CapturesCoroutineStub>()
        coEvery { stub.startCapture(any(), any()) } returns
            CapturesOuterClass.CommandResult_StartCaptureResponse.newBuilder()
                .setIsAuthorized(true)
                .setResponse(
                    CapturesOuterClass.StartCaptureResponse.newBuilder()
                        .addMessages(message("source is unreachable", 0, 0))
                        .build()
                )
                .build()

        assertEquals("source is unreachable", serviceFor(stub).start(captureId).single().message)
    }

    @Test
    fun `stopping and deleting name the capture`() = runBlocking {
        val stub = mockk<CapturesGrpcKt.CapturesCoroutineStub>()
        val stopped = slot<CapturesOuterClass.StopCaptureRequest>()
        val deleted = slot<CapturesOuterClass.DeleteCaptureRequest>()
        coEvery { stub.stopCapture(capture(stopped), any()) } returns
            CapturesOuterClass.CommandResult.newBuilder().setIsAuthorized(true).build()
        coEvery { stub.deleteCapture(capture(deleted), any()) } returns
            CapturesOuterClass.CommandResult.newBuilder().setIsAuthorized(true).build()

        serviceFor(stub).stop(captureId)
        serviceFor(stub).delete(captureId)

        assertEquals(captureId.toContractGuid(), stopped.captured.captureId)
        assertEquals(captureId.toContractGuid(), deleted.captured.captureId)
    }
}
