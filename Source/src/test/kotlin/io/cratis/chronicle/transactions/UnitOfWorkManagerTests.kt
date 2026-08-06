// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.transactions

import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.correlation.CorrelationId
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UnitOfWorkManagerTests {

    private fun manager(): UnitOfWorkManager = UnitOfWorkManager(mockk<IEventStore>())

    @Test
    fun `current throws NoUnitOfWorkHasBeenStarted when nothing has been begun`() {
        val manager = manager()

        assertThrows(NoUnitOfWorkHasBeenStarted::class.java) { manager.current }
    }

    @Test
    fun `hasCurrent is false initially and true after begin`() {
        val manager = manager()

        assertFalse(manager.hasCurrent)
        manager.begin()
        assertTrue(manager.hasCurrent)
    }

    @Test
    fun `begin with no correlation id auto-generates one`() {
        val manager = manager()

        val unitOfWork = manager.begin()

        assertNotEquals(CorrelationId.notSet, unitOfWork.correlationId)
        assertSame(unitOfWork, manager.current)
    }

    @Test
    fun `begin with an explicit correlation id uses it and makes it discoverable through tryGetFor`() {
        val manager = manager()
        val correlationId = CorrelationId.create()

        val unitOfWork = manager.begin(correlationId)

        assertEquals(correlationId, unitOfWork.correlationId)
        assertSame(unitOfWork, manager.tryGetFor(correlationId))
    }

    @Test
    fun `tryGetFor returns null for a correlation id with no unit of work`() {
        val manager = manager()

        assertNull(manager.tryGetFor(CorrelationId.create()))
    }

    @Test
    fun `setCurrent makes the given unit of work current and discoverable through tryGetFor`() {
        val manager = manager()
        val correlationId = CorrelationId.create()
        val unitOfWork = UnitOfWork(correlationId = correlationId, eventStore = mockk())

        manager.setCurrent(unitOfWork)

        assertSame(unitOfWork, manager.current)
        assertSame(unitOfWork, manager.tryGetFor(correlationId))
    }

    @Test
    fun `current and tryGetFor no longer resolve the unit of work once it has committed`() = runBlocking {
        val manager = manager()
        val unitOfWork = manager.begin()
        val correlationId = unitOfWork.correlationId

        unitOfWork.commit()

        assertFalse(manager.hasCurrent)
        assertNull(manager.tryGetFor(correlationId))
        assertThrows(NoUnitOfWorkHasBeenStarted::class.java) { manager.current }
    }

    @Test
    fun `completing a unit of work that is no longer current does not clear a different, newer current`() = runBlocking {
        val manager = manager()
        val first = manager.begin()
        val second = manager.begin()

        first.commit()

        assertSame(second, manager.current)
    }

    @Test
    fun `begin replaces the current unit of work for this thread`() {
        val manager = manager()
        val first = manager.begin()
        val second = manager.begin()

        assertSame(second, manager.current)
        assertNotEquals(first.correlationId, second.correlationId)
    }

    private fun assertNotEquals(unexpected: Any?, actual: Any?) {
        org.junit.jupiter.api.Assertions.assertNotEquals(unexpected, actual)
    }
}
