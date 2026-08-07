// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.captures

import kotlinx.coroutines.flow.Flow

/**
 * The captures an event store holds: sources outside Chronicle, pulled in and appended as events.
 *
 * A capture is defined by a Capture Declaration Language document. The kernel parses it, holds it,
 * and runs it - so the client's job is to get declarations to the kernel and to report back what it
 * made of them.
 *
 * Most applications never touch this directly: declare an [ICapture] and discovery saves and starts
 * it on connect. Reach for this when the declaration is not known at build time - one being written
 * in an editor and validated as it is typed, or read from configuration - or to inspect and control
 * what is running.
 */
interface ICapturesService {
    /**
     * Every capture the event store holds, running or not.
     *
     * @return The captures, in whatever order the kernel returns them.
     */
    suspend fun getAll(): List<Capture>

    /**
     * Every capture, re-emitted whenever any of them changes.
     *
     * Use this rather than polling [getAll] to keep a view of what is running up to date.
     *
     * @return A flow emitting the full set on every change.
     */
    fun observeAll(): Flow<List<Capture>>

    /**
     * Saves [declaration] under [id], replacing whatever was held there.
     *
     * Saving does not start the capture. A rejected declaration changes nothing.
     *
     * @param id The identifier to hold the capture under.
     * @param declaration The Capture Declaration Language document.
     * @return The capture as saved, or what is wrong with the declaration.
     */
    suspend fun save(id: String, declaration: String): CaptureDeclarationResult

    /**
     * Checks [declaration] without saving anything.
     *
     * This is what an editor calls as the declaration is typed.
     *
     * @param declaration The document to check.
     * @return Empty when the kernel is happy with it; otherwise what is wrong, with line and column.
     */
    suspend fun validate(declaration: String): List<CaptureValidationMessage>

    /**
     * Starts the capture held under [id], so it begins appending what its source produces.
     *
     * @param id The capture to start.
     * @return Empty when it started; otherwise why it could not.
     */
    suspend fun start(id: String): List<CaptureValidationMessage>

    /**
     * Stops the capture held under [id]. It stays saved, and can be started again.
     *
     * @param id The capture to stop.
     */
    suspend fun stop(id: String)

    /**
     * Removes the capture held under [id] entirely.
     *
     * Events it already appended are facts and stay exactly where they are.
     *
     * @param id The capture to delete.
     */
    suspend fun delete(id: String)
}
