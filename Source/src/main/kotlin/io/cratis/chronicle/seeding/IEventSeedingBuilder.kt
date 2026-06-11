// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.seeding

interface IEventSeedingBuilder {
    fun forEventSource(eventSourceId: String, events: List<Any>): IEventSeedingBuilder
}
