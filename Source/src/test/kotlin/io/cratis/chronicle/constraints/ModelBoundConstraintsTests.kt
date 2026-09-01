// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.constraints

import io.cratis.chronicle.events.EventType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@EventType
private data class ModelBoundProjectInitialized(val name: String)

@EventType
@Unique
private data class ModelBoundWorkspaceClaimed(val slug: String)

@EventType
private data class ModelBoundUserRegistered(@Unique(id = "ModelBoundUniqueEmail") val email: String)

@EventType
private data class ModelBoundUserEmailChanged(@Unique(id = "ModelBoundUniqueEmail") val newEmail: String)

@EventType
private data class ModelBoundUsernameClaimed(@Unique(id = "ModelBoundUniqueUsername") val username: String)

@EventType
@RemoveConstraint("ModelBoundUniqueEmail")
private data class ModelBoundUserRemoved(val userId: String)

@EventType
@RemoveConstraint("ModelBoundUniqueEmail")
@RemoveConstraint("ModelBoundUniqueUsername")
private data class ModelBoundUserFullyRemoved(val userId: String)

@EventType
private data class ModelBoundUserDeactivated(val userId: String)

@EventType
private data class ModelBoundSharedNameOwner(@Unique(id = "ModelBoundSharedName") val value: String)

@EventType
@RemoveConstraint("ModelBoundSharedName")
private data class ModelBoundFirstReleaser(val id: String)

@EventType
@RemoveConstraint("ModelBoundSharedName")
private data class ModelBoundSecondReleaser(val id: String)

@EventType
private data class ModelBoundProjectCreated(@Unique val name: String, val description: String)

class ModelBoundConstraintsTests {

    @Test
    fun `buildFor produces no constraints for event types with no Unique or RemoveConstraint`() {
        val constraints = ModelBoundConstraints.buildFor(listOf(ModelBoundProjectInitialized::class))
        assertTrue(constraints.isEmpty())
    }

    @Test
    fun `buildFor produces a UniqueEventType constraint for a class-level Unique`() {
        val constraints = ModelBoundConstraints.buildFor(listOf(ModelBoundWorkspaceClaimed::class))

        val constraint = constraints.single()
        assertEquals("ModelBoundWorkspaceClaimed", constraint.name)
        assertEquals(2, constraint.typeValue) // UniqueEventType
        assertEquals(listOf("ModelBoundWorkspaceClaimed"), constraint.definition.value1.eventTypeIdsList)
    }

    @Test
    fun `buildFor produces a Unique constraint for a property-level Unique, defaulting the name to the property`() {
        val constraints = ModelBoundConstraints.buildFor(listOf(ModelBoundProjectCreated::class))

        val constraint = constraints.single()
        assertEquals("name", constraint.name)
        assertEquals(1, constraint.typeValue) // Unique
        val eventDefinition = constraint.definition.value0.eventDefinitionsList.single()
        assertEquals("ModelBoundProjectCreated", eventDefinition.eventTypeId)
        assertEquals(listOf("name"), eventDefinition.propertiesList)
    }

    @Test
    fun `buildFor groups property-level Unique annotations sharing an explicit id into one constraint`() {
        val constraints = ModelBoundConstraints.buildFor(
            listOf(ModelBoundUserRegistered::class, ModelBoundUserEmailChanged::class)
        )

        val constraint = constraints.single { it.name == "ModelBoundUniqueEmail" }
        val eventTypeIds = constraint.definition.value0.eventDefinitionsList.map { it.eventTypeId }
        assertEquals(setOf("ModelBoundUserRegistered", "ModelBoundUserEmailChanged"), eventTypeIds.toSet())
    }

    @Test
    fun `buildFor sets removedWith from an event type carrying a matching RemoveConstraint`() {
        val constraints = ModelBoundConstraints.buildFor(
            listOf(ModelBoundUserRegistered::class, ModelBoundUserRemoved::class)
        )

        val constraint = constraints.single { it.name == "ModelBoundUniqueEmail" }
        assertEquals("ModelBoundUserRemoved", constraint.removedWith)
    }

    @Test
    fun `buildFor leaves removedWith unset when nothing releases the constraint`() {
        val constraints = ModelBoundConstraints.buildFor(
            listOf(ModelBoundUserRegistered::class, ModelBoundUserDeactivated::class)
        )

        val constraint = constraints.single { it.name == "ModelBoundUniqueEmail" }
        assertEquals("", constraint.removedWith)
    }

    @Test
    fun `buildFor lets one event type release more than one constraint`() {
        val constraints = ModelBoundConstraints.buildFor(
            listOf(ModelBoundUserRegistered::class, ModelBoundUsernameClaimed::class, ModelBoundUserFullyRemoved::class)
        )

        assertEquals("ModelBoundUserFullyRemoved", constraints.single { it.name == "ModelBoundUniqueEmail" }.removedWith)
        assertEquals("ModelBoundUserFullyRemoved", constraints.single { it.name == "ModelBoundUniqueUsername" }.removedWith)
    }

    @Test
    fun `buildFor keeps only the first releasing event type when more than one shares a RemoveConstraint name`() {
        val constraints = ModelBoundConstraints.buildFor(
            listOf(ModelBoundSharedNameOwner::class, ModelBoundFirstReleaser::class, ModelBoundSecondReleaser::class)
        )

        val constraint = constraints.single { it.name == "ModelBoundSharedName" }
        assertEquals("ModelBoundFirstReleaser", constraint.removedWith)
    }
}
