// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

@file:JvmName("ClientArtifactsEventSources")

package io.cratis.chronicle.artifacts

import kotlin.reflect.KClass

/**
 * The classes annotated with [io.cratis.chronicle.eventSources.EventSource], or none when the implementation
 * predates event sources and does not implement [IEventSourceArtifacts].
 */
val IClientArtifacts.eventSources: List<KClass<*>>
    get() = (this as? IEventSourceArtifacts)?.eventSources ?: emptyList()
