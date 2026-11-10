// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.imports

import Cratis.Chronicle.Contracts.Imports.ImportsGrpcKt
import Cratis.Chronicle.Contracts.Imports.ImportsOuterClass as Contracts
import io.cratis.chronicle.eventSequences.ChronicleCommandRejected
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.StatusRuntimeException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/**
 * The calls an import makes to the kernel's `Imports` service, with the command and query envelopes
 * unwrapped. A rejected or unauthorized call throws [ChronicleCommandRejected].
 */
internal interface ImportCalls {
    suspend fun begin(request: Contracts.BeginImportRequest): Contracts.BeginImportResponse

    suspend fun addChunk(request: Contracts.AddImportChunkRequest): Contracts.AddImportChunkResponse

    suspend fun complete(request: Contracts.CompleteImportRequest): Contracts.AbandonImportResponse

    suspend fun abandon(request: Contracts.AbandonImportRequest): Contracts.AbandonImportResponse

    /** Reads the import, or returns `null` when the kernel does not know it. */
    suspend fun get(request: Contracts.GetImportRequest): Contracts.ImportProgressResponse?

    fun observe(request: Contracts.ObserveImportRequest): Flow<Contracts.ImportProgressResponse>
}

/** [ImportCalls] over the generated gRPC stub. */
internal class GrpcImportCalls(private val stub: ImportsGrpcKt.ImportsCoroutineStub) : ImportCalls {
    override suspend fun begin(request: Contracts.BeginImportRequest): Contracts.BeginImportResponse {
        val result = try {
            stub.beginImport(request)
        } catch (e: Exception) {
            if (statusCodeOf(e) == Status.Code.UNIMPLEMENTED) throw ImportsNotSupported("The kernel does not support importing events.")
            throw e
        }
        ensureSuccess("begin import", result.isAuthorized, result.authorizationFailureReason, result.exceptionMessagesList)
        return result.response
    }

    override suspend fun addChunk(request: Contracts.AddImportChunkRequest): Contracts.AddImportChunkResponse {
        val result = stub.addImportChunk(request)
        ensureSuccess("add import chunk", result.isAuthorized, result.authorizationFailureReason, result.exceptionMessagesList)
        return result.response
    }

    override suspend fun complete(request: Contracts.CompleteImportRequest): Contracts.AbandonImportResponse {
        val result = stub.completeImport(request)
        ensureSuccess("complete import", result.isAuthorized, result.authorizationFailureReason, result.exceptionMessagesList)
        return result.response
    }

    override suspend fun abandon(request: Contracts.AbandonImportRequest): Contracts.AbandonImportResponse {
        val result = stub.abandonImport(request)
        ensureSuccess("abandon import", result.isAuthorized, result.authorizationFailureReason, result.exceptionMessagesList)
        return result.response
    }

    override suspend fun get(request: Contracts.GetImportRequest): Contracts.ImportProgressResponse? {
        val result = stub.getImport(request)
        ensureSuccess("get import", result.isAuthorized, "", result.exceptionMessagesList)
        return if (result.hasData()) result.data else null
    }

    override fun observe(request: Contracts.ObserveImportRequest): Flow<Contracts.ImportProgressResponse> =
        stub.observeImport(request).filter { it.hasData() }.map { it.data }

    private fun ensureSuccess(operation: String, isAuthorized: Boolean, authorizationFailureReason: String, exceptionMessages: List<String>) {
        if (!isAuthorized) throw ChronicleCommandRejected(operation, authorizationFailureReason.ifEmpty { "not authorized" })
        if (exceptionMessages.isNotEmpty()) throw ChronicleCommandRejected(operation, exceptionMessages.joinToString("; "))
    }
}

/** The gRPC status code of [error], or `null` when it is not a gRPC failure. */
internal fun statusCodeOf(error: Throwable): Status.Code? = when (error) {
    is StatusException -> error.status.code
    is StatusRuntimeException -> error.status.code
    else -> null
}
