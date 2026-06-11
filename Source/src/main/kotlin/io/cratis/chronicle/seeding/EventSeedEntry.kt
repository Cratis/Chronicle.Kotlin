// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.seeding

data class EventSeedEntry(
    val eventSourceId: String,
    val events: List<Any>
)
