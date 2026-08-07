// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.readModels

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private fun changeset(
    readModel: EmployeeProfile?,
    changeType: ReadModelChangeType,
    modelKey: String = "employee-1"
) = ReadModelChangeset(
    namespace = "default",
    modelKey = modelKey,
    readModel = readModel,
    removed = changeType == ReadModelChangeType.Removed,
    changeType = changeType,
    eventSequenceNumber = 3,
    occurred = null,
    correlationId = null
)

class ReadModelReactorMethodTests {

    class RecordingReactor : IReadModelReactor {
        var instance: EmployeeProfile? = null
        var seenChangeset: ReadModelChangeset<*>? = null

        fun added(employee: EmployeeProfile) {
            instance = employee
        }

        fun modified(employee: EmployeeProfile, changeset: ReadModelChangeset<EmployeeProfile>) {
            instance = employee
            seenChangeset = changeset
        }

        fun removed(employee: EmployeeProfile?) {
            instance = employee
        }
    }

    class BatchReactor : IReadModelReactor {
        var batch: List<EmployeeProfile>? = null

        fun added(employees: List<EmployeeProfile>) {
            batch = employees
        }

        fun removed(employees: List<EmployeeProfile>) {
            batch = employees
        }
    }

    class SideEffectReactor : IReadModelReactor {
        fun added(employee: EmployeeProfile) = "welcomed ${employee.name}"
    }

    private fun methodFor(reactorClass: kotlin.reflect.KClass<*>, changeType: ReadModelChangeType) =
        ReadModelReactorHandlers.from(reactorClass).resolve(EmployeeProfile::class, changeType).single()

    @Test
    fun `invoking a handler passes the changed instance`() = runBlocking {
        val reactor = RecordingReactor()
        val employee = EmployeeProfile("Ada")

        methodFor(RecordingReactor::class, ReadModelChangeType.Added)
            .invoke(reactor, changeset(employee, ReadModelChangeType.Added))

        assertSame(employee, reactor.instance)
    }

    @Test
    fun `invoking a handler passes the changeset when it asks for one`() = runBlocking {
        val reactor = RecordingReactor()
        val change = changeset(EmployeeProfile("Ada"), ReadModelChangeType.Modified)

        methodFor(RecordingReactor::class, ReadModelChangeType.Modified).invoke(reactor, change)

        assertSame(change, reactor.seenChangeset)
    }

    @Test
    fun `invoking a removal handler passes no instance`() = runBlocking {
        val reactor = RecordingReactor()

        methodFor(RecordingReactor::class, ReadModelChangeType.Removed)
            .invoke(reactor, changeset(null, ReadModelChangeType.Removed))

        assertNull(reactor.instance)
    }

    @Test
    fun `invoking a collection handler passes the instance as a single element list`() = runBlocking {
        val reactor = BatchReactor()
        val employee = EmployeeProfile("Ada")

        methodFor(BatchReactor::class, ReadModelChangeType.Added)
            .invoke(reactor, changeset(employee, ReadModelChangeType.Added))

        assertEquals(listOf(employee), reactor.batch)
    }

    @Test
    fun `invoking a collection handler passes an empty list when the instance is gone`() = runBlocking {
        val reactor = BatchReactor()

        methodFor(BatchReactor::class, ReadModelChangeType.Removed)
            .invoke(reactor, changeset(null, ReadModelChangeType.Removed))

        assertTrue(reactor.batch!!.isEmpty())
    }

    @Test
    fun `invoking a handler returns what it returned`() = runBlocking {
        val result = methodFor(SideEffectReactor::class, ReadModelChangeType.Added)
            .invoke(SideEffectReactor(), changeset(EmployeeProfile("Ada"), ReadModelChangeType.Added))

        assertEquals("welcomed Ada", result)
    }
}
