// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import Cratis.Chronicle.Contracts.Observation.Reactors.ObservationReactors
import Cratis.Chronicle.Contracts.Observation.Reactors.ReactorsGrpcKt
import io.cratis.chronicle.connection.ConnectionLifecycle
import io.cratis.chronicle.eventSequences.IEventLog
import io.cratis.chronicle.events.EventType
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@EventType
private data class TailedEvent(val value: String)

@Reactor
@StartAtTail
private class TailReactor {
    fun received(event: TailedEvent) = Unit
}

@Reactor
private class HistoryReactor {
    fun received(event: TailedEvent) = Unit
}

class ReactorStartAtTailRegistrationTests {
    @Test
    fun `a reactor marked to start at the tail asks the kernel to do so`() {
        assertTrue(registrationOf(TailReactor()).reactor.startAtTail)
    }

    @Test
    fun `a reactor that is not marked does not ask the kernel to skip history`() {
        assertFalse(registrationOf(HistoryReactor()).reactor.startAtTail)
    }

    private fun registrationOf(reactor: Any): ObservationReactors.RegisterReactor = runBlocking {
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
        val job = ReactorsService("store", "default", lifecycle, stub, mockk<IEventLog>()).register(reactor)
        try {
            withTimeout(2_000) { registered.await() }.content.value0
        } finally {
            job.cancel()
        }
    }
}
