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
    /** MongoDB sink — the default when no sink type is configured. */
    const val MONGODB = "MongoDB"

    /** SQL sink — covers PostgreSQL, SQL Server, and SQLite. */
    const val SQL = "SQL"

    /** In-memory sink — read models are not persisted across restarts. */
    const val IN_MEMORY = "InMemory"
}
