// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.constraints

import Cratis.Chronicle.Contracts.Events.Constraints.ConstraintsGrpcKt
import Cratis.Chronicle.Contracts.Events.Constraints.EventsConstraints
import com.google.protobuf.Empty
import io.cratis.chronicle.events.EventType
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@EventType
private data class ConstraintScopeEmailSet(val email: String)

@Constraint
private class UnscopedUniqueEmail : IConstraint {
    override fun define(builder: IConstraintBuilder) {
        builder.unique { it.on(ConstraintScopeEmailSet::class, ConstraintScopeEmailSet::email) }
    }
}

@Constraint
private class PerEventSourceTypeUniqueEmail : IConstraint {
    override fun define(builder: IConstraintBuilder) {
        builder.perEventSourceType().unique { it.on(ConstraintScopeEmailSet::class, ConstraintScopeEmailSet::email) }
    }
}

@Constraint
private class PerStreamTypeAndIdUniqueEmail : IConstraint {
    override fun define(builder: IConstraintBuilder) {
        builder.perEventStreamType().perEventStreamId().unique { it.on(ConstraintScopeEmailSet::class, ConstraintScopeEmailSet::email) }
    }
}

class ConstraintsServiceTests {

    @Test
    fun `register sends an empty scope for a constraint with no scoping calls`() = runBlocking {
        val stub = mockk<ConstraintsGrpcKt.ConstraintsCoroutineStub>()
        val request = slot<EventsConstraints.RegisterConstraintsRequest>()
        coEvery { stub.register(capture(request), any()) } returns Empty.getDefaultInstance()

        val service = ConstraintsService("my-store", stub)
        service.register(UnscopedUniqueEmail())

        val scope = request.captured.constraintsList.single().scope
        assertEquals("", scope.eventSourceType)
        assertEquals("", scope.eventStreamType)
        assertEquals("", scope.eventStreamId)
    }

    @Test
    fun `register sends a populated event source type on the scope when perEventSourceType is used`() = runBlocking {
        val stub = mockk<ConstraintsGrpcKt.ConstraintsCoroutineStub>()
        val request = slot<EventsConstraints.RegisterConstraintsRequest>()
        coEvery { stub.register(capture(request), any()) } returns Empty.getDefaultInstance()

        val service = ConstraintsService("my-store", stub)
        service.register(PerEventSourceTypeUniqueEmail())

        val scope = request.captured.constraintsList.single().scope
        assertTrue(scope.eventSourceType.isNotEmpty())
        assertEquals("", scope.eventStreamType)
        assertEquals("", scope.eventStreamId)
    }

    @Test
    fun `register sends populated stream type and stream id on the scope when both are combined`() = runBlocking {
        val stub = mockk<ConstraintsGrpcKt.ConstraintsCoroutineStub>()
        val request = slot<EventsConstraints.RegisterConstraintsRequest>()
        coEvery { stub.register(capture(request), any()) } returns Empty.getDefaultInstance()

        val service = ConstraintsService("my-store", stub)
        service.register(PerStreamTypeAndIdUniqueEmail())

        val scope = request.captured.constraintsList.single().scope
        assertEquals("", scope.eventSourceType)
        assertTrue(scope.eventStreamType.isNotEmpty())
        assertTrue(scope.eventStreamId.isNotEmpty())
    }
}
