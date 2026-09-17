// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.testing

import io.cratis.chronicle.eventSequences.AppendOptions

/** The server test double's resolved route, not a production SDK routing policy. */
internal data class InMemoryEventRoute(
    val eventSourceType: String,
    val eventStreamType: String,
    val eventStreamId: String
) {
    companion object {
        /** Emulates the kernel's missing/empty defaults while preserving every nonempty route. */
        fun resolve(options: AppendOptions?): InMemoryEventRoute = InMemoryEventRoute(
            eventSourceType = options?.eventSourceType.orEmpty().ifEmpty { "Default" },
            eventStreamType = options?.eventStreamType.orEmpty().ifEmpty { "All" },
            eventStreamId = options?.eventStreamId.orEmpty().ifEmpty { "Default" }
        )
    }
}
