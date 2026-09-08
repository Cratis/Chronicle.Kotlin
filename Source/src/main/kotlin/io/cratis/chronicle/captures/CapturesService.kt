// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.captures

import Cratis.Chronicle.Contracts.Captures.CapturesGrpcKt
import Cratis.Chronicle.Contracts.Captures.CapturesOuterClass
import bcl.Bcl
import java.util.UUID
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
        stub.getCaptures(getRequest()).dataList.map { it.toClient() }

    override fun observeAll(): Flow<List<Capture>> {
        val request = CapturesOuterClass.ObserveCapturesRequest.newBuilder()
            .setEventStore(eventStoreName)
            .build()
        return stub.observeCaptures(request).map { response -> response.dataList.map { it.toClient() } }
    }

    override suspend fun save(id: String, declaration: String): CaptureDeclarationResult {
        val request = CapturesOuterClass.SaveCaptureRequest.newBuilder()
            .setEventStore(eventStoreName)
            .setId(id.toContractsGuid())
            .setDeclaration(declaration)
            .build()

        val response = stub.saveCapture(request).ensureSuccess("save capture")
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
        val request = CapturesOuterClass.ValidateCaptureDeclarationRequest.newBuilder()
            .setEventStore(eventStoreName)
            .setDeclaration(declaration)
            .build()

        return stub.validateCaptureDeclaration(request).ensureSuccess("validate capture declaration").messagesList.map { it.toClient() }
    }

    override suspend fun start(id: String): List<CaptureValidationMessage> {
        val request = CapturesOuterClass.StartCaptureRequest.newBuilder()
            .setEventStore(eventStoreName)
            .setCaptureId(id.toContractsGuid())
            .build()

        return stub.startCapture(request).ensureSuccess("start capture").messagesList.map { it.toClient() }
    }

    override suspend fun stop(id: String) {
        val request = CapturesOuterClass.StopCaptureRequest.newBuilder()
            .setEventStore(eventStoreName)
            .setCaptureId(id.toContractsGuid())
            .build()

        stub.stopCapture(request).ensureSuccess("stop capture")
    }

    override suspend fun delete(id: String) {
        val request = CapturesOuterClass.DeleteCaptureRequest.newBuilder()
            .setEventStore(eventStoreName)
            .setCaptureId(id.toContractsGuid())
            .build()

        stub.deleteCapture(request).ensureSuccess("delete capture")
    }

    private fun getRequest(): CapturesOuterClass.GetCapturesRequest =
        CapturesOuterClass.GetCapturesRequest.newBuilder()
            .setEventStore(eventStoreName)
            .build()

    private fun CapturesOuterClass.CaptureDetailsResponse.toClient() = Capture(
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

private fun String.toContractsGuid(): Bcl.Guid {
    // bcl.Guid: lo = first 8 bytes, hi = second 8 bytes, little-endian.
    // Java UUID.mostSignificantBits and leastSignificantBits are big-endian, so reverse each half.
    val uuid = UUID.fromString(this)
    return Bcl.Guid.newBuilder()
        .setLo(java.lang.Long.reverseBytes(uuid.mostSignificantBits))
        .setHi(java.lang.Long.reverseBytes(uuid.leastSignificantBits))
        .build()
}

private fun ensureSuccessMessage(operation: String, isAuthorized: Boolean, exceptionMessages: List<String>) {
    if (!isAuthorized) throw io.cratis.chronicle.eventSequences.ChronicleCommandRejected(operation, "not authorized")
    if (exceptionMessages.isNotEmpty()) throw io.cratis.chronicle.eventSequences.ChronicleCommandRejected(operation, exceptionMessages.joinToString("; "))
}

private fun CapturesOuterClass.CommandResult.ensureSuccess(operation: String) =
    ensureSuccessMessage(operation, isAuthorized, exceptionMessagesList)

private fun CapturesOuterClass.CommandResult_SaveCaptureResponse.ensureSuccess(operation: String): CapturesOuterClass.SaveCaptureResponse {
    ensureSuccessMessage(operation, isAuthorized, exceptionMessagesList)
    return response
}

private fun CapturesOuterClass.CommandResult_StartCaptureResponse.ensureSuccess(operation: String): CapturesOuterClass.StartCaptureResponse {
    ensureSuccessMessage(operation, isAuthorized, exceptionMessagesList)
    return response
}

private fun CapturesOuterClass.CommandResult_ValidateCaptureDeclarationResponse.ensureSuccess(operation: String): CapturesOuterClass.ValidateCaptureDeclarationResponse {
    ensureSuccessMessage(operation, isAuthorized, exceptionMessagesList)
    return response
}
