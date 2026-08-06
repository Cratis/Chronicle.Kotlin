// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import io.cratis.chronicle.eventSequences.EventSequenceId
import io.cratis.chronicle.observation.EventSequence
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ProjectionRegistrationTests {

    private data class Shipment(val id: String = "", val carrier: String = "")

    class UnannotatedProjection

    @Projection
    class DefaultedProjection

    @Projection(id = "custom-id", eventSequence = "outbox")
    class ConfiguredProjection

    @EventSequence("outbox")
    class StandaloneEventSequenceProjection

    @Projection(eventSequence = "from-parameter")
    @EventSequence("from-standalone")
    class DoublyConfiguredProjection

    @Test
    fun `id defaults to the class simple name`() {
        assertEquals("DefaultedProjection", ProjectionRegistration.from(DefaultedProjection::class).id)
    }

    @Test
    fun `id defaults to the class simple name without the annotation`() {
        assertEquals("UnannotatedProjection", ProjectionRegistration.from(UnannotatedProjection::class).id)
    }

    @Test
    fun `explicit id is used`() {
        assertEquals("custom-id", ProjectionRegistration.from(ConfiguredProjection::class).id)
    }

    @Test
    fun `event sequence defaults to the event log`() {
        assertEquals(
            EventSequenceId.eventLog.value,
            ProjectionRegistration.from(DefaultedProjection::class).eventSequenceId
        )
    }

    @Test
    fun `event sequence defaults to the event log without the annotation`() {
        assertEquals(
            EventSequenceId.eventLog.value,
            ProjectionRegistration.from(UnannotatedProjection::class).eventSequenceId
        )
    }

    @Test
    fun `explicit event sequence is used`() {
        assertEquals("outbox", ProjectionRegistration.from(ConfiguredProjection::class).eventSequenceId)
    }

    @Test
    fun `standalone event sequence annotation is used`() {
        assertEquals(
            "outbox",
            ProjectionRegistration.from(StandaloneEventSequenceProjection::class).eventSequenceId
        )
    }

    @Test
    fun `standalone event sequence annotation wins over the parameter`() {
        // Matching the .NET client, where [EventSequence] on the class takes priority over the
        // event sequence declared on [Projection].
        assertEquals(
            "from-standalone",
            ProjectionRegistration.from(DoublyConfiguredProjection::class).eventSequenceId
        )
    }

    @Test
    fun `java standalone event sequence annotation is read`() {
        assertEquals(
            "outbox",
            ProjectionRegistration.from(JavaEventSequenceProjection::class).eventSequenceId
        )
    }

    @Test
    fun `a model-bound read model is read the same way as a declarative projection`() {
        // The annotation sits on the read model itself for a model-bound projection, so the same
        // resolution has to hold for a data class as for a projection class.
        assertEquals("Shipment", ProjectionRegistration.from(Shipment::class).id)
    }
}
