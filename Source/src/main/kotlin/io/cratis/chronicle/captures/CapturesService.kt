// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.captures

import Cratis.Chronicle.Contracts.Captures.CapturesGrpcKt
import Cratis.Chronicle.Contracts.Captures.CapturesOuterClass
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implements [ICapturesService] by talking to the kernel over gRPC.
 *
 * @param eventStoreName The event store the captures belong to.
 * @param stub The stub captures are managed through.
 */
class CapturesService(
    private val eventStoreName: String,
    private val stub: CapturesGrpcKt.CapturesCoroutineStub
) : ICapturesService {

    override suspend fun getAll(): List<Capture> =
        stub.getCaptures(getRequest()).itemsList.map { it.toClient() }

    override fun observeAll(): Flow<List<Capture>> =
        stub.observeCaptures(getRequest()).map { response -> response.itemsList.map { it.toClient() } }

    override suspend fun save(id: String, declaration: String): CaptureDeclarationResult {
        val request = CapturesOuterClass.SaveCapture.newBuilder()
            .setEventStore(eventStoreName)
            .setId(id)
            .setDeclaration(declaration)
            .build()

        val response = stub.save(request)
        val messages = response.messagesList.map { it.toClient() }

        // The kernel answers a rejected declaration with messages and no capture, so an absent
        // capture is what says it did not take - not the presence of messages, which a declaration
        // can carry while still being accepted.
        return if (response.hasCapture()) {
            CaptureDeclarationResult.Accepted(response.capture.toClient(), messages)
        } else {
            CaptureDeclarationResult.Rejected(messages)
        }
    }

    override suspend fun validate(declaration: String): List<CaptureValidationMessage> {
        val request = CapturesOuterClass.ValidateCaptureDeclaration.newBuilder()
            .setEventStore(eventStoreName)
            .setDeclaration(declaration)
            .build()

        return stub.validateDeclaration(request).messagesList.map { it.toClient() }
    }

    override suspend fun start(id: String): List<CaptureValidationMessage> {
        val request = CapturesOuterClass.StartCapture.newBuilder()
            .setEventStore(eventStoreName)
            .setId(id)
            .build()

        return stub.start(request).messagesList.map { it.toClient() }
    }

    override suspend fun stop(id: String) {
        val request = CapturesOuterClass.StopCapture.newBuilder()
            .setEventStore(eventStoreName)
            .setId(id)
            .build()

        stub.stop(request)
    }

    override suspend fun delete(id: String) {
        val request = CapturesOuterClass.DeleteCapture.newBuilder()
            .setEventStore(eventStoreName)
            .setId(id)
            .build()

        stub.delete(request)
    }

    private fun getRequest(): CapturesOuterClass.GetCapturesRequest =
        CapturesOuterClass.GetCapturesRequest.newBuilder()
            .setEventStore(eventStoreName)
            .build()

    private fun CapturesOuterClass.Capture.toClient() = Capture(
        id = id,
        name = name,
        declaration = declaration,
        status = when (status) {
            CapturesOuterClass.CaptureStatus.Started -> CaptureStatus.Started
            else -> CaptureStatus.Stopped
        }
    )

    private fun CapturesOuterClass.CaptureValidationMessage.toClient() =
        CaptureValidationMessage(message, line, column)
}
