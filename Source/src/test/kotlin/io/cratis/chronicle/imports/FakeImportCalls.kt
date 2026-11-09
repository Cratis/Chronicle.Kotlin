// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.imports

import Cratis.Chronicle.Contracts.Imports.ImportsOuterClass as Contracts
import io.cratis.chronicle.toBclGuid
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Stands in for the kernel's `Imports` service: records every call, and lets a spec decide how a chunk
 * is answered and what the import reads as.
 */
internal class FakeImportCalls(
    var beginError: String = "",
    var maxChunkEvents: Int = 0,
    var maxChunkBytes: Int = 0,
) : ImportCalls {
    val importId: UUID = UUID.randomUUID()
    val begun = CopyOnWriteArrayList<Contracts.BeginImportRequest>()
    val chunks = CopyOnWriteArrayList<Contracts.AddImportChunkRequest>()
    val completed = CopyOnWriteArrayList<Contracts.CompleteImportRequest>()
    val abandoned = CopyOnWriteArrayList<Contracts.AbandonImportRequest>()
    val inFlight = AtomicInteger()
    val mostInFlight = AtomicInteger()
    val progressUpdates = MutableSharedFlow<Contracts.ImportProgressResponse>(replay = 16)

    /** Answers a chunk; runs while the chunk counts as in flight. */
    var onChunk: suspend (Contracts.AddImportChunkRequest) -> Contracts.AddImportChunkResponse = { accepted(it) }

    /** What the import reads as; `null` when the kernel does not know it. */
    var reads: () -> Contracts.ImportProgressResponse? = { progress(Contracts.ImportState.Completed) }

    override suspend fun begin(request: Contracts.BeginImportRequest): Contracts.BeginImportResponse {
        begun.add(request)
        return Contracts.BeginImportResponse.newBuilder()
            .setImportId(if (request.hasImportId()) request.importId else importId.toBclGuid())
            .setState(Contracts.ImportState.Open)
            .setMaxChunkEvents(maxChunkEvents)
            .setMaxChunkBytes(maxChunkBytes)
            .setError(beginError)
            .build()
    }

    override suspend fun addChunk(request: Contracts.AddImportChunkRequest): Contracts.AddImportChunkResponse {
        val now = inFlight.incrementAndGet()
        mostInFlight.accumulateAndGet(now) { a, b -> maxOf(a, b) }
        try {
            chunks.add(request)
            return onChunk(request)
        } finally {
            inFlight.decrementAndGet()
        }
    }

    override suspend fun complete(request: Contracts.CompleteImportRequest): Contracts.AbandonImportResponse {
        completed.add(request)
        return Contracts.AbandonImportResponse.newBuilder().setImportId(request.importId).setState(Contracts.ImportState.Committing).build()
    }

    override suspend fun abandon(request: Contracts.AbandonImportRequest): Contracts.AbandonImportResponse {
        abandoned.add(request)
        return Contracts.AbandonImportResponse.newBuilder().setImportId(request.importId).setState(Contracts.ImportState.Abandoned).build()
    }

    override suspend fun get(request: Contracts.GetImportRequest): Contracts.ImportProgressResponse? = reads()

    override fun observe(request: Contracts.ObserveImportRequest): Flow<Contracts.ImportProgressResponse> = progressUpdates

    fun progress(state: Contracts.ImportState, configure: Contracts.ImportProgressResponse.Builder.() -> Unit = {}): Contracts.ImportProgressResponse =
        Contracts.ImportProgressResponse.newBuilder()
            .setImportId(importId.toBclGuid())
            .setEventSequenceId("event-log")
            .setState(state)
            .apply(configure)
            .build()

    /** The events of every chunk sent, in chunk order, by their content. */
    fun sentContents(): List<String> = chunks.sortedBy { it.chunkIndex }.flatMap { chunk -> chunk.eventsList.map { it.content } }

    companion object {
        fun accepted(request: Contracts.AddImportChunkRequest): Contracts.AddImportChunkResponse =
            Contracts.AddImportChunkResponse.newBuilder()
                .setChunkIndex(request.chunkIndex)
                .setEventsReceived(request.eventsCount)
                .setEventsStaged(request.eventsCount)
                .build()

        /** A wire event carrying [content] and nothing else, as the converter under test would produce. */
        fun wireEvent(content: String): Contracts.ImportEvent = Contracts.ImportEvent.newBuilder().setContent(content).build()
    }
}
