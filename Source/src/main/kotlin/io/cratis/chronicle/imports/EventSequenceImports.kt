// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.imports

import Cratis.Chronicle.Contracts.Imports.ImportsOuterClass as Contracts
import io.cratis.chronicle.eventSequences.EventForEventSourceId
import io.cratis.chronicle.toBclGuid
import io.cratis.chronicle.toUuid
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Begins, reattaches to and runs imports into one event sequence.
 *
 * @param calls The kernel's `Imports` service.
 * @param eventStore The event store.
 * @param namespace The namespace.
 * @param eventSequenceId The event sequence imported into.
 * @param convert Converts an event to its wire form, resolving its type and event source routing.
 * @param createImport Creates the session; replaced in specs to shorten delays.
 */
internal class EventSequenceImports(
    private val calls: ImportCalls,
    private val eventStore: String,
    private val namespace: String,
    private val eventSequenceId: String,
    private val convert: suspend (EventForEventSourceId) -> Contracts.ImportEvent,
    private val createImport: (ImportId, ImportOptions, Int, Int, Int, Long) -> IEventImport = { id, options, events, bytes, chunks, acknowledged ->
        EventImport(calls, eventStore, namespace, id, options, events, bytes, convert, chunks, acknowledged)
    },
) {
    /** Begins a new import, or one under [ImportOptions.importId]. */
    suspend fun begin(options: ImportOptions): IEventImport {
        val session = calls.begin(
            Contracts.BeginImportRequest.newBuilder().apply {
                eventStore = this@EventSequenceImports.eventStore
                namespace = this@EventSequenceImports.namespace
                eventSequenceId = this@EventSequenceImports.eventSequenceId
                onInvalid = options.onInvalid.toContract()
                options.importId?.let { importId = it.value.toBclGuid() }
            }.build(),
        )
        if (session.error.isNotEmpty()) throw ImportCouldNotBegin(session.error)

        return createImport(ImportId(session.importId.toUuid()), options, session.maxChunkEvents, session.maxChunkBytes, 0, 0)
    }

    /** Reattaches to an existing import, continuing after the chunks the kernel acknowledged. */
    suspend fun reattach(importId: ImportId, options: ImportOptions): IEventImport {
        val progress = calls.get(
            Contracts.GetImportRequest.newBuilder()
                .setEventStore(eventStore)
                .setNamespace(namespace)
                .setImportId(importId.value.toBclGuid())
                .build(),
        ) ?: throw ImportCouldNotBegin("Import '$importId' does not exist.")
        if (progress.eventSequenceId != eventSequenceId) {
            throw ImportCouldNotBegin("Import '$importId' imports into event sequence '${progress.eventSequenceId}', not '$eventSequenceId'.")
        }

        return createImport(
            importId,
            options.copy(onInvalid = progress.onInvalid.toClient(), importId = importId),
            progress.maxChunkEvents,
            progress.maxChunkBytes,
            progress.acknowledgedChunks,
            progress.acknowledgedEvents,
        )
    }

    /**
     * Runs a whole import: begins it, adds what [add] adds and completes it, reporting progress to
     * [onProgress] while it runs. A failure to observe progress does not fail the import.
     */
    suspend fun run(
        options: ImportOptions,
        onProgress: FlowCollector<ImportProgress>?,
        add: suspend (IEventImport) -> Unit,
    ): ImportResult = coroutineScope {
        val session = begin(options)
        val reporting = onProgress?.let { collector -> launch { session.progress.catch { }.collect(collector) } }
        try {
            add(session)
            session.complete()
        } finally {
            reporting?.cancel()
            withContext(NonCancellable) { session.close() }
        }
    }
}
