// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.readModels

import io.cratis.chronicle.observation.InvalidHandlerSignature
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

data class EmployeeProfile(val name: String)

data class DepartmentSummary(val headcount: Int)

class ReadModelReactorHandlersTests {

    class EveryChangeReactor : IReadModelReactor {
        fun added(@Suppress("UNUSED_PARAMETER") employee: EmployeeProfile) = Unit
        fun modified(@Suppress("UNUSED_PARAMETER") employee: EmployeeProfile) = Unit
        fun removed(@Suppress("UNUSED_PARAMETER") employee: EmployeeProfile?) = Unit
    }

    class AdditionOnlyReactor : IReadModelReactor {
        fun added(@Suppress("UNUSED_PARAMETER") employee: EmployeeProfile) = Unit
        fun somethingElse(@Suppress("UNUSED_PARAMETER") employee: EmployeeProfile) = Unit
    }

    class TwoReadModelReactor : IReadModelReactor {
        fun added(@Suppress("UNUSED_PARAMETER") employee: EmployeeProfile) = Unit
        fun modified(@Suppress("UNUSED_PARAMETER") department: DepartmentSummary) = Unit
    }

    class CollectionReactor : IReadModelReactor {
        fun added(@Suppress("UNUSED_PARAMETER") employees: List<EmployeeProfile>) = Unit
    }

    class PascalCaseReactor : IReadModelReactor {
        @Suppress("FunctionName")
        fun Added(@Suppress("UNUSED_PARAMETER") employee: EmployeeProfile) = Unit
    }

    class SuspendingReactor : IReadModelReactor {
        @Suppress("RedundantSuspendModifier")
        suspend fun added(@Suppress("UNUSED_PARAMETER") employee: EmployeeProfile) = Unit
    }

    class TooManyParametersReactor : IReadModelReactor {
        fun added(
            @Suppress("UNUSED_PARAMETER") employee: EmployeeProfile,
            @Suppress("UNUSED_PARAMETER") changeset: ReadModelChangeset<EmployeeProfile>,
            @Suppress("UNUSED_PARAMETER") extra: String
        ) = Unit
    }

    class WrongSecondParameterReactor : IReadModelReactor {
        fun added(
            @Suppress("UNUSED_PARAMETER") employee: EmployeeProfile,
            @Suppress("UNUSED_PARAMETER") reason: String
        ) = Unit
    }

    class NonNullableRemovalReactor : IReadModelReactor {
        fun removed(@Suppress("UNUSED_PARAMETER") employee: EmployeeProfile) = Unit
    }

    @Test
    fun `added handler is resolved for an addition`() {
        val handlers = ReadModelReactorHandlers.from(EveryChangeReactor::class)
        val resolved = handlers.resolve(EmployeeProfile::class, ReadModelChangeType.Added)
        assertEquals("added", resolved.single().function.name)
    }

    @Test
    fun `modified handler is resolved for a modification`() {
        val handlers = ReadModelReactorHandlers.from(EveryChangeReactor::class)
        val resolved = handlers.resolve(EmployeeProfile::class, ReadModelChangeType.Modified)
        assertEquals("modified", resolved.single().function.name)
    }

    @Test
    fun `removed handler is resolved for a removal`() {
        val handlers = ReadModelReactorHandlers.from(EveryChangeReactor::class)
        val resolved = handlers.resolve(EmployeeProfile::class, ReadModelChangeType.Removed)
        assertEquals("removed", resolved.single().function.name)
    }

    @Test
    fun `a change type with no handler resolves to nothing`() {
        val handlers = ReadModelReactorHandlers.from(AdditionOnlyReactor::class)
        assertTrue(handlers.resolve(EmployeeProfile::class, ReadModelChangeType.Removed).isEmpty())
    }

    @Test
    fun `a read model the reactor does not handle resolves to nothing`() {
        val handlers = ReadModelReactorHandlers.from(AdditionOnlyReactor::class)
        assertTrue(handlers.resolve(DepartmentSummary::class, ReadModelChangeType.Added).isEmpty())
    }

    @Test
    fun `methods that are not handlers are ignored`() {
        val handlers = ReadModelReactorHandlers.from(AdditionOnlyReactor::class)
        assertEquals(1, handlers.resolve(EmployeeProfile::class, ReadModelChangeType.Added).size)
    }

    @Test
    fun `every handled read model is watched`() {
        val handlers = ReadModelReactorHandlers.from(TwoReadModelReactor::class)
        assertEquals(setOf(EmployeeProfile::class, DepartmentSummary::class), handlers.readModelClasses)
    }

    @Test
    fun `a collection handler is registered against the element type`() {
        val handlers = ReadModelReactorHandlers.from(CollectionReactor::class)
        assertEquals(setOf(EmployeeProfile::class), handlers.readModelClasses)
        assertTrue(handlers.resolve(EmployeeProfile::class, ReadModelChangeType.Added).single().isCollection)
    }

    @Test
    fun `a pascal case method name is still a handler`() {
        val handlers = ReadModelReactorHandlers.from(PascalCaseReactor::class)
        assertEquals("Added", handlers.resolve(EmployeeProfile::class, ReadModelChangeType.Added).single().function.name)
    }

    @Test
    fun `a suspending handler is rejected`() {
        assertThrows<InvalidHandlerSignature> { ReadModelReactorHandlers.from(SuspendingReactor::class) }
    }

    @Test
    fun `a handler taking too many parameters is rejected`() {
        assertThrows<InvalidHandlerSignature> { ReadModelReactorHandlers.from(TooManyParametersReactor::class) }
    }

    @Test
    fun `a handler whose second parameter is not a changeset is rejected`() {
        assertThrows<InvalidHandlerSignature> { ReadModelReactorHandlers.from(WrongSecondParameterReactor::class) }
    }

    @Test
    fun `a kotlin removal handler that cannot accept a missing instance is rejected`() {
        assertThrows<InvalidHandlerSignature> { ReadModelReactorHandlers.from(NonNullableRemovalReactor::class) }
    }

    @Test
    fun `a java authored reactor is discovered for every change`() {
        val handlers = ReadModelReactorHandlers.from(JavaReadModelReactor::class)
        assertEquals(setOf(JavaEmployeeProfile::class), handlers.readModelClasses)
        ReadModelChangeType.entries.forEach { changeType ->
            assertEquals(1, handlers.resolve(JavaEmployeeProfile::class, changeType).size, "for $changeType")
        }
    }

    @Test
    fun `a java authored removal handler is accepted despite carrying no nullability`() {
        val handlers = ReadModelReactorHandlers.from(JavaReadModelReactor::class)
        val removed = handlers.resolve(JavaEmployeeProfile::class, ReadModelChangeType.Removed).single()
        assertTrue(!removed.takesNullableReadModel && !removed.isDeclaredInKotlin)
    }

    @Test
    fun `a java authored changeset parameter is recognized`() {
        val handlers = ReadModelReactorHandlers.from(JavaReadModelReactor::class)
        assertTrue(handlers.resolve(JavaEmployeeProfile::class, ReadModelChangeType.Modified).single().takesChangeset)
    }

    @Test
    fun `a java authored collection handler is registered against the element type`() {
        val handlers = ReadModelReactorHandlers.from(JavaCollectionReadModelReactor::class)
        assertEquals(setOf(JavaEmployeeProfile::class), handlers.readModelClasses)
    }
}
