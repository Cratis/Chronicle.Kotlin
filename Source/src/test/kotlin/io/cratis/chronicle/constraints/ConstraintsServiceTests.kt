// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.constraints

import Cratis.Chronicle.Contracts.Events.Constraints.ConstraintsGrpcKt
import Cratis.Chronicle.Contracts.Events.Constraints.EventsConstraints
import com.google.protobuf.Empty
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.eventSequences.ConstraintViolation
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@EventType
private data class ConstraintScopeEmailSet(val email: String)

@EventType
private data class ModelBoundRegistrationUserRegistered(
    @Unique(id = "ModelBoundRegistrationUniqueEmail", message = "Email {email} is taken") val email: String
)

@EventType
@Unique(message = "Already registered")
private data class ModelBoundRegistrationClassUnique(val name: String)

@Constraint(id = "DeclarativeEmail")
private class DeclarativeEmail : IConstraint {
    override fun define(builder: IConstraintBuilder) {
        builder.unique { it.on(ConstraintScopeEmailSet::class, ConstraintScopeEmailSet::email).withMessage("Taken: {email}") }
    }
}

@Constraint(id = "DeclarativeEventType")
private class DeclarativeEventType : IConstraint {
    override fun define(builder: IConstraintBuilder) {
        builder.uniqueFor(ConstraintScopeEmailSet::class, "Already claimed")
    }
}


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
    fun `declarative messages resolve details and preserve unknown or empty messages`() = runBlocking {
        val stub = mockk<ConstraintsGrpcKt.ConstraintsCoroutineStub>()
        coEvery { stub.register(any(), any()) } returns Empty.getDefaultInstance()
        val service = ConstraintsService("my-store", stub)

        service.register(DeclarativeEmail(), DeclarativeEventType(), UnscopedUniqueEmail())

        assertEquals("Taken: a@b.com", service.resolveMessageFor(
            ConstraintViolation("DeclarativeEmail", "kernel message", mapOf("email" to "a@b.com"))
        ).message)
        assertEquals("Already claimed", service.resolveMessageFor(
            ConstraintViolation("DeclarativeEventType", "kernel message")
        ).message)
        assertEquals("kernel message", service.resolveMessageFor(
            ConstraintViolation("UnscopedUniqueEmail", "kernel message")
        ).message)
        assertEquals("kernel message", service.resolveMessageFor(
            ConstraintViolation("closed-stream", "kernel message")
        ).message)
    }

    @Test
    fun `model-bound messages use the class and property constraint names`() = runBlocking {
        val stub = mockk<ConstraintsGrpcKt.ConstraintsCoroutineStub>()
        coEvery { stub.register(any(), any()) } returns Empty.getDefaultInstance()
        val service = ConstraintsService("my-store", stub)

        service.registerModelBound(listOf(ModelBoundRegistrationUserRegistered::class, ModelBoundRegistrationClassUnique::class))

        assertEquals("Email a@b.com is taken", service.resolveMessageFor(
            ConstraintViolation("ModelBoundRegistrationUniqueEmail", "kernel", mapOf("email" to "a@b.com"))
        ).message)
        assertEquals("Already registered", service.resolveMessageFor(
            ConstraintViolation("ModelBoundRegistrationClassUnique", "kernel")
        ).message)
    }

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

    @Test
    fun `registerModelBound sends a request built from the given event types' Unique annotations`() = runBlocking {
        val stub = mockk<ConstraintsGrpcKt.ConstraintsCoroutineStub>()
        val request = slot<EventsConstraints.RegisterConstraintsRequest>()
        coEvery { stub.register(capture(request), any()) } returns Empty.getDefaultInstance()

        val service = ConstraintsService("my-store", stub)
        service.registerModelBound(listOf(ModelBoundRegistrationUserRegistered::class))

        val constraint = request.captured.constraintsList.single()
        assertEquals("ModelBoundRegistrationUniqueEmail", constraint.name)
    }

    @Test
    fun `registerModelBound sends nothing when no event type carries Unique or RemoveConstraint`() = runBlocking {
        val stub = mockk<ConstraintsGrpcKt.ConstraintsCoroutineStub>()

        val service = ConstraintsService("my-store", stub)
        service.registerModelBound(listOf(ConstraintScopeEmailSet::class))

        coVerify(exactly = 0) { stub.register(any(), any()) }
    }
}
