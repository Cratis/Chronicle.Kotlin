// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.artifacts

import kotlin.reflect.KClass

/**
 * Defines every kind of Chronicle artifact an application can bring to the table.
 *
 * An implementation answers the question "what does this application consist of?" — the event types
 * it records, the observers that watch them, the read models they produce, and the rules that guard
 * them. The client uses that answer to register everything with the kernel on connect, so no artifact
 * has to be registered by hand.
 *
 * [ClientArtifacts] discovers all of this by scanning the classpath. [KnownClientArtifacts] takes an
 * explicit list instead, for when discovery is unwanted or the set is known up front.
 */
interface IClientArtifacts {
    /** Every class annotated with [io.cratis.chronicle.events.EventType]. */
    val eventTypes: List<KClass<*>>

    /** Every class implementing [io.cratis.chronicle.events.migrations.IEventTypeMigration]. */
    val eventTypeMigrations: List<KClass<*>>

    /** Every class annotated with [io.cratis.chronicle.readModels.ReadModel]. */
    val readModels: List<KClass<*>>

    /** Every class implementing [io.cratis.chronicle.projections.IProjectionFor]. */
    val projections: List<KClass<*>>

    /** Every read model carrying [io.cratis.chronicle.projections.FromEvent] — a model-bound projection. */
    val modelBoundProjections: List<KClass<*>>

    /** Every class annotated with [io.cratis.chronicle.observation.Reactor]. */
    val reactors: List<KClass<*>>

    /** Every class annotated with [io.cratis.chronicle.observation.Reducer]. */
    val reducers: List<KClass<*>>

    /** Every class implementing [io.cratis.chronicle.constraints.IConstraint]. */
    val constraints: List<KClass<*>>

    /** Every class implementing [io.cratis.chronicle.seeding.ICanSeedEvents]. */
    val eventSeeders: List<KClass<*>>

    /** Every class implementing [io.cratis.chronicle.webhooks.IWebhookDefiner]. */
    val webhooks: List<KClass<*>>
}
