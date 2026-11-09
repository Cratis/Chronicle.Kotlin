// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.sinks

/**
 * Well-known sink type identifiers used when registering reducers and projections with Chronicle.
 *
 * The sink type tells Chronicle which storage backend should receive the projected read model state.
 * Pass the desired constant as [io.cratis.chronicle.ChronicleOptions.defaultSinkTypeId] or via the
 * `CHRONICLE_SINK_TYPE` environment variable.
 */
object WellKnownSinkTypes {
    /** No materialized sink — passive read models are projected when read. */
    const val NONE = "None"

    /** MongoDB sink — the default when no sink type is configured. */
    const val MONGODB = "MongoDB"

    /** SQL sink — covers PostgreSQL, SQL Server, and SQLite. */
    const val SQL = "SQL"

    /** In-memory sink — read models are not persisted across restarts. */
    const val IN_MEMORY = "InMemory"

    /** Neo4j graph sink — the default for graph projections, which project events onto a property graph. */
    const val NEO4J = "Neo4j"

    /** In-memory graph sink — keeps the graph in the kernel's memory, for development and tests. */
    const val IN_MEMORY_GRAPH = "InMemoryGraph"
}
