// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.readModels

import io.cratis.chronicle.eventSequences.AppendResult
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.eventSequences.IEventLog
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

private fun <T : Any> changeOf(
    readModel: T?,
    changeType: ReadModelChangeType,
    modelKey: String = "employee-1"
) = ReadModelChangeset(
    namespace = "default",
    modelKey = modelKey,
    readModel = readModel,
    removed = changeType == ReadModelChangeType.Removed,
    changeType = changeType,
    eventSequenceNumber = 1,
    occurred = null,
    correlationId = null
)

/** Waits for a reactor running on a background dispatcher to have done its work. */
private suspend fun awaitUntil(condition: () -> Boolean) = withTimeout(WAIT_TIMEOUT_MS) {
    while (!condition()) delay(POLL_INTERVAL_MS)
}

private const val WAIT_TIMEOUT_MS = 5_000L
private const val POLL_INTERVAL_MS = 10L

class ReadModelReactorsTests {

    class CountingReactor : IReadModelReactor {
        val seen = mutableListOf<String>()

        fun added(employee: EmployeeProfile) {
            seen.add("added:${employee.name}")
        }

        fun removed(employee: EmployeeProfile?, changeset: ReadModelChangeset<EmployeeProfile>) {
            seen.add("removed:${changeset.modelKey}")
        }
    }

    class TwoReadModelReactor : IReadModelReactor {
        fun added(@Suppress("UNUSED_PARAMETER") employee: EmployeeProfile) = Unit
        fun added(@Suppress("UNUSED_PARAMETER") department: DepartmentSummary) = Unit
    }

    class WelcomingReactor : IReadModelReactor {
        fun added(employee: EmployeeProfile) = EmployeeWelcomed(employee.name)
    }

    private fun eventLog(): IEventLog = mockk<IEventLog>().also {
        coEvery { it.append(any(), any(), any()) } returns
            AppendResult(EventSequenceNumber(0), emptyList(), emptyList(), true)
    }

    @Test
    fun `registering a reactor watches every read model it handles`() {
        val readModels = mockk<IReadModelsService>()
        every { readModels.watch(EmployeeProfile::class) } returns emptyFlow()
        every { readModels.watch(DepartmentSummary::class) } returns emptyFlow()
        val reactors = ReadModelReactors(readModels, eventLog())

        try {
            reactors.register(TwoReadModelReactor())

            verify(timeout = WAIT_TIMEOUT_MS) { readModels.watch(EmployeeProfile::class) }
            verify(timeout = WAIT_TIMEOUT_MS) { readModels.watch(DepartmentSummary::class) }
        } finally {
            reactors.stop()
        }
    }

    @Test
    fun `an addition is dispatched to the matching handler`() = runBlocking {
        val readModels = mockk<IReadModelsService>()
        every { readModels.watch(EmployeeProfile::class) } returns
            flowOf(changeOf(EmployeeProfile("Ada"), ReadModelChangeType.Added))
        val reactor = CountingReactor()
        val reactors = ReadModelReactors(readModels, eventLog())

        try {
            reactors.register(reactor)
            awaitUntil { reactor.seen.isNotEmpty() }
        } finally {
            reactors.stop()
        }

        assertEquals("added:Ada", reactor.seen.first())
    }

    @Test
    fun `a removal is dispatched with the key of the instance that went away`() = runBlocking {
        val readModels = mockk<IReadModelsService>()
        every { readModels.watch(EmployeeProfile::class) } returns
            flowOf(changeOf<EmployeeProfile>(null, ReadModelChangeType.Removed, "employee-7"))
        val reactor = CountingReactor()
        val reactors = ReadModelReactors(readModels, eventLog())

        try {
            reactors.register(reactor)
            awaitUntil { reactor.seen.isNotEmpty() }
        } finally {
            reactors.stop()
        }

        assertEquals("removed:employee-7", reactor.seen.first())
    }

    @Test
    fun `an event returned by a handler is appended against the changed instance key`() = runBlocking {
        val readModels = mockk<IReadModelsService>()
        every { readModels.watch(EmployeeProfile::class) } returns
            flowOf(changeOf(EmployeeProfile("Ada"), ReadModelChangeType.Added, "employee-9"))
        val eventLog = eventLog()
        val reactors = ReadModelReactors(readModels, eventLog)

        try {
            reactors.register(WelcomingReactor())
            coVerify(timeout = WAIT_TIMEOUT_MS) { eventLog.append("employee-9", EmployeeWelcomed("Ada"), null) }
        } finally {
            reactors.stop()
        }
    }

    @Test
    fun `a java authored reactor receives changes`() = runBlocking {
        val readModels = mockk<IReadModelsService>()
        every { readModels.watch(JavaEmployeeProfile::class) } returns
            flowOf(changeOf(JavaEmployeeProfile("Ada"), ReadModelChangeType.Added))
        val reactor = JavaReadModelReactor()
        val reactors = ReadModelReactors(readModels, eventLog())

        try {
            reactors.register(reactor)
            awaitUntil { reactor.calls.isNotEmpty() }
        } finally {
            reactors.stop()
        }

        assertEquals("added:Ada", reactor.calls.first())
    }
}
