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
private data class ReplayabilityEvent(val value: String)

@Reactor
@OnceOnly
private class OnceOnlyRegistrationReactor {
    fun received(event: ReplayabilityEvent) = Unit
}

@Reactor
private class ReplayableRegistrationReactor {
    fun received(event: ReplayabilityEvent) = Unit
}

@Reactor
private class OnceOnlyHandlerRegistrationReactor {
    @OnceOnly
    fun received(event: ReplayabilityEvent) = Unit
}

class ReactorReplayabilityRegistrationTests {
    @Test
    fun `a class-level OnceOnly reactor asks the kernel not to replay it`() {
        assertTrue(registrationOf(OnceOnlyRegistrationReactor()).reactor.isNotReplayable)
    }

    @Test
    fun `a class-level OnceOnly reactor still sends the legacy flag as false`() {
        assertFalse(registrationOf(OnceOnlyRegistrationReactor()).reactor.isReplayable)
    }

    @Test
    fun `a plain reactor does not ask the kernel to prevent replay`() {
        assertFalse(registrationOf(ReplayableRegistrationReactor()).reactor.isNotReplayable)
    }

    @Test
    fun `a plain reactor still sends the legacy flag as true`() {
        assertTrue(registrationOf(ReplayableRegistrationReactor()).reactor.isReplayable)
    }

    @Test
    fun `method-level OnceOnly does not ask the kernel to prevent reactor replay`() {
        assertFalse(registrationOf(OnceOnlyHandlerRegistrationReactor()).reactor.isNotReplayable)
    }

    @Test
    fun `method-level OnceOnly leaves the reactor replayable`() {
        assertTrue(registrationOf(OnceOnlyHandlerRegistrationReactor()).reactor.isReplayable)
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
