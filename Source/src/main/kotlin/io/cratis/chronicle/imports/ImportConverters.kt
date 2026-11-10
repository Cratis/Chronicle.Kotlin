// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.imports

import Cratis.Chronicle.Contracts.Imports.ImportsOuterClass as Contracts
import io.cratis.chronicle.toUuid

internal fun Contracts.ImportState.toClient(): ImportState = when (this) {
    Contracts.ImportState.Open -> ImportState.Open
    Contracts.ImportState.Staging -> ImportState.Staging
    Contracts.ImportState.Validating -> ImportState.Validating
    Contracts.ImportState.Committing -> ImportState.Committing
    Contracts.ImportState.Completed -> ImportState.Completed
    Contracts.ImportState.Failed -> ImportState.Failed
    Contracts.ImportState.Abandoned -> ImportState.Abandoned
    Contracts.ImportState.UNRECOGNIZED -> ImportState.Failed
}

internal fun OnInvalidEvent.toContract(): Contracts.OnInvalidEvent = when (this) {
    OnInvalidEvent.Reject -> Contracts.OnInvalidEvent.Reject
    OnInvalidEvent.Skip -> Contracts.OnInvalidEvent.Skip
}

internal fun Contracts.OnInvalidEvent.toClient(): OnInvalidEvent =
    if (this == Contracts.OnInvalidEvent.Skip) OnInvalidEvent.Skip else OnInvalidEvent.Reject

internal fun Contracts.ImportProgressResponse.toProgress(): ImportProgress = ImportProgress(
    importId = ImportId(importId.toUuid()),
    state = state.toClient(),
    eventsReceived = eventsReceived,
    eventsStaged = eventsStaged,
    eventsCommitted = eventsCommitted,
    alreadyAppended = alreadyAppended,
    eventsRejected = rejectedCount,
)

/** The wire carries no "absent" for a sequence number, so none is reported until an event is committed. */
internal fun Contracts.ImportProgressResponse.toResult(): ImportResult = ImportResult(
    importId = ImportId(importId.toUuid()),
    state = state.toClient(),
    eventsStaged = eventsStaged,
    eventsCommitted = eventsCommitted,
    alreadyAppended = alreadyAppended,
    firstSequenceNumber = if (eventsCommitted > 0) firstSequenceNumber else null,
    lastSequenceNumber = if (eventsCommitted > 0) lastSequenceNumber else null,
    rejectedCount = rejectedCount,
    rejections = rejectionsList.map { ImportRejection(it.index, it.eventType, it.reasonsList.toList()) },
    failureReason = failureReason.ifEmpty { null },
)

/**
 * Measures an event as the kernel counts it against its chunk byte limit: the content, tags, named tags,
 * causation and idempotency key, as UTF-8.
 */
internal fun Contracts.ImportEvent.sizeInBytes(): Int {
    var bytes = content.utf8Size() + idempotencyKey.utf8Size()
    tagsList.forEach { bytes += it.utf8Size() }
    namedTagsList.forEach { bytes += it.name.utf8Size() + it.value.utf8Size() }
    causationList.forEach { causation ->
        bytes += causation.type.utf8Size()
        causation.propertiesMap.forEach { (name, value) -> bytes += name.utf8Size() + value.utf8Size() }
    }
    return bytes
}

private fun String.utf8Size(): Int = toByteArray(Charsets.UTF_8).size

internal fun java.time.Instant.toImportContract(): Contracts.SerializableDateTimeOffset =
    Contracts.SerializableDateTimeOffset.newBuilder()
        .setValue(java.time.format.DateTimeFormatter.ISO_INSTANT.format(this))
        .build()

internal fun io.cratis.chronicle.auditing.Causation.toImportContract(): Contracts.Causation =
    Contracts.Causation.newBuilder()
        .setOccurred(timestamp.toImportContract())
        .setType(type.name)
        .putAllProperties(properties)
        .build()

internal fun io.cratis.chronicle.identity.Identity.toImportContract(): Contracts.Identity {
    val builder = Contracts.Identity.newBuilder()
        .setSubject(subject)
        .setName(name)
        .setUserName(userName)
    onBehalfOf?.let { builder.setOnBehalfOf(it.toImportContract()) }
    return builder.build()
}
