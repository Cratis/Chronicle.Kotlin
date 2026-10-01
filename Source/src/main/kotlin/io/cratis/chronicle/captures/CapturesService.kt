// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.captures

import Cratis.Chronicle.Contracts.Captures.CapturesGrpcKt
import Cratis.Chronicle.Contracts.Captures.CapturesOuterClass
import bcl.Bcl
import io.cratis.chronicle.toBclGuid
import io.cratis.chronicle.toTransposedUuid
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
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

    private val logger = System.getLogger(CapturesService::class.java.name)
    private val reportedLegacyIds = ConcurrentHashMap.newKeySet<UUID>()

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
            .setId(resolve(id))
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
            .setCaptureId(resolve(id))
            .build()

        return stub.startCapture(request).ensureSuccess("start capture").messagesList.map { it.toClient() }
    }

    override suspend fun stop(id: String) {
        val request = CapturesOuterClass.StopCaptureRequest.newBuilder()
            .setEventStore(eventStoreName)
            .setCaptureId(resolve(id))
            .build()

        stub.stopCapture(request).ensureSuccess("stop capture")
    }

    override suspend fun delete(id: String) {
        val request = CapturesOuterClass.DeleteCaptureRequest.newBuilder()
            .setEventStore(eventStoreName)
            .setCaptureId(resolve(id))
            .build()

        stub.deleteCapture(request).ensureSuccess("delete capture")
    }

    /**
     * The wire id to address the capture held for [id] by.
     *
     * Earlier clients sent capture ids with the first three groups byte-reversed, so the kernel holds a
     * capture they saved under the transposed form of the id the application supplied. Changing that
     * identity would mean a new capture with no record of what it has already seen, so the capture is
     * kept where it is and addressed there: the correct id when the kernel holds it, otherwise the
     * transposed id when the kernel holds that, otherwise the correct id, which is what a new capture is
     * created under. The kernel has no lookup by id, so this lists the event store's captures.
     */
    private suspend fun resolve(id: String): Bcl.Guid {
        val correct = UUID.fromString(id)
        val legacy = correct.toTransposedUuid()
        if (legacy == correct) return correct.toBclGuid()

        val held = stub.getCaptures(getRequest()).dataList.mapNotNull { runCatching { UUID.fromString(it.id) }.getOrNull() }.toSet()
        if (correct in held || legacy !in held) return correct.toBclGuid()

        if (reportedLegacyIds.add(correct)) {
            logger.log(
                System.Logger.Level.INFO,
                "Capture $correct is held under its legacy id $legacy, written by an earlier client version; it is addressed by that id."
            )
        }
        return legacy.toBclGuid()
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
