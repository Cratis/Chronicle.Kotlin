// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import Cratis.Chronicle.Contracts.Observation.Reducers.ObservationReducers
import Cratis.Chronicle.Contracts.Observation.Reducers.ReducersGrpcKt
import Cratis.Chronicle.Contracts.Observation.Reactors.ObservationReactors
import Cratis.Chronicle.Contracts.Observation.Reactors.ReactorsGrpcKt
import io.cratis.chronicle.connection.ConnectionLifecycle
import io.cratis.chronicle.eventSequences.IEventLog
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.EVENT_SOURCE_ID_EXPRESSION
import io.cratis.chronicle.readModels.Passive
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@EventType
private data class KeyedEvent(val value: String)

@Passive
private data class PassiveKeyedModel(val value: String)

private class KeyedReducer {
    fun received(event: KeyedEvent) = PassiveKeyedModel(event.value)
}

private class KeyedReactor {
    fun received(event: KeyedEvent) = Unit
}

class ObserverKeyExpressionTests {
    @Test
    fun `reducer registrations use the event source expression`() = runBlocking {
        val lifecycle = ConnectionLifecycle()
        val stub = mockk<ReducersGrpcKt.ReducersCoroutineStub>()
        val registered = CompletableDeferred<ObservationReducers.ReducerMessage>()
        every { stub.observe(any(), any()) } answers {
            val requests = firstArg<Flow<ObservationReducers.ReducerMessage>>()
            kotlinx.coroutines.flow.flow {
                registered.complete(requests.first())
            }
        }
        lifecycle.markConnected(lifecycle.connectionId)
        val job = ReducersService("store", "default", lifecycle, stub).register(KeyedReducer())
        try {
            val message = withTimeout(2_000) { registered.await() }
            assertEquals(EVENT_SOURCE_ID_EXPRESSION, message.content.value0.reducer.eventTypesList.single().key)
            assertEquals(false, message.content.value0.reducer.isActive)
        } finally {
            job.cancel()
        }
    }

    @Test
    fun `reactor registrations use the event source expression`() = runBlocking {
        val lifecycle = ConnectionLifecycle()
        val stub = mockk<ReactorsGrpcKt.ReactorsCoroutineStub>()
        val registered = CompletableDeferred<ObservationReactors.ReactorMessage>()
        every { stub.observe(any(), any()) } answers {
            val requests = firstArg<Flow<ObservationReactors.ReactorMessage>>()
            kotlinx.coroutines.flow.flow {
                registered.complete(requests.first())
            }
        }
        lifecycle.markConnected(lifecycle.connectionId)
        val job = ReactorsService("store", "default", lifecycle, stub, mockk<IEventLog>()).register(KeyedReactor())
        try {
            val message = withTimeout(2_000) { registered.await() }
            assertEquals(EVENT_SOURCE_ID_EXPRESSION, message.content.value0.reactor.eventTypesList.single().key)
        } finally {
            job.cancel()
        }
    }
}
