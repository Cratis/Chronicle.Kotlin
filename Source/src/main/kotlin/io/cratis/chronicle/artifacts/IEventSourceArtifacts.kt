// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.artifacts

import kotlin.reflect.KClass

/**
 * Optional capability of an [IClientArtifacts]: it also reports the classes carrying event source definitions.
 *
 * It is a separate interface, rather than a member of [IClientArtifacts], so that implementations written
 * before event sources existed - in Kotlin or Java, compiled or not - keep compiling and linking unchanged.
 * [ClientArtifacts] and [KnownClientArtifacts] implement it; read the classes through the
 * `IClientArtifacts.eventSources` extension, which reports none for an implementation without it.
 */
interface IEventSourceArtifacts {
    /** Classes annotated with [io.cratis.chronicle.eventSources.EventSource], carrying event source and stream definitions. */
    val eventSources: List<KClass<*>>
}
