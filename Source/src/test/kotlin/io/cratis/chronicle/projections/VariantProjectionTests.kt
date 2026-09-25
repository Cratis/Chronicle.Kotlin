// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import Cratis.Chronicle.Contracts.Projections.ProjectionsGrpcKt
import Cratis.Chronicle.Contracts.Projections.ProjectionsOuterClass
import com.google.protobuf.Empty
import io.cratis.chronicle.events.EventType
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlin.reflect.KClass
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// --- Model-bound variant fixtures ---

@EventType
private data class MbVariantIssueCreated(val title: String)

@EventType
private data class MbVariantPullRequestCreated(val pullRequestUrl: String)

@EventType
private data class MbVariantBuildCompleted(val buildStatus: String)

@EventType
private data class MbVariantTitleChanged(val title: String)

/** Anchors the logical identity shared by [MbVariantBacklogItem] and [MbVariantPullRequestItem]. */
private class MbVariantWorkItem

@VariantOf(MbVariantWorkItem::class, key = "id")
@EntersOn(MbVariantIssueCreated::class)
@FromEvent(MbVariantIssueCreated::class)
private data class MbVariantBacklogItem(
    @FromEventSourceId
    val id: String = "",
    @SetFrom("title", MbVariantIssueCreated::class)
    val title: String = ""
)

@VariantOf(MbVariantWorkItem::class, key = "id")
@EntersOn(MbVariantPullRequestCreated::class)
@FromEvent(MbVariantPullRequestCreated::class)
@FromEvent(MbVariantBuildCompleted::class)
private data class MbVariantPullRequestItem(
    @FromEventSourceId
    val id: String = "",
    val title: String = "",
    @SetFrom("pullRequestUrl", MbVariantPullRequestCreated::class)
    val pullRequestUrl: String = "",
    @SetFrom("buildStatus", MbVariantBuildCompleted::class)
    val buildStatus: String = ""
)

@GlobalFor(MbVariantWorkItem::class)
@FromEvent(MbVariantTitleChanged::class)
private data class MbVariantSharedHandlers(
    @SetFrom("title", MbVariantTitleChanged::class)
    val title: String = ""
)

@VariantOf(MbVariantWorkItem::class, key = "id")
private data class MbVariantMissingEntersOn(
    @FromEventSourceId
    val id: String = ""
)

@GlobalFor(MbVariantWorkItem::class)
@FromEvent(MbVariantTitleChanged::class)
private data class MbVariantMismatchedGlobalHandler(
    @SetFrom("title", MbVariantTitleChanged::class)
    val subject: String = ""
)

// --- Declarative variant fixtures ---

private class DecVariantWorkItem

private data class DecVariantBacklogItem(val id: String = "", val title: String = "")
private data class DecVariantPullRequestItem(val id: String = "", val pullRequestUrl: String = "", val buildStatus: String = "")

private class DecVariantBacklogItemProjection : IProjectionFor<DecVariantBacklogItem> {
    override fun define(builder: IProjectionBuilderFor<DecVariantBacklogItem>) {
        builder
            .variantOf(DecVariantWorkItem::class, DecVariantBacklogItem::id)
            .entersOn(MbVariantIssueCreated::class)
    }
}

private class DecVariantPullRequestItemProjection : IProjectionFor<DecVariantPullRequestItem> {
    override fun define(builder: IProjectionBuilderFor<DecVariantPullRequestItem>) {
        builder
            .variantOf(DecVariantWorkItem::class, DecVariantPullRequestItem::id)
            .entersOn(MbVariantPullRequestCreated::class)
            .from(MbVariantBuildCompleted::class)
    }
}

class VariantProjectionTests {

    private fun register(vararg projections: Any): List<ProjectionsOuterClass.ProjectionDefinition> {
        val stub = mockk<ProjectionsGrpcKt.ProjectionsCoroutineStub>()
        val request = slot<ProjectionsOuterClass.RegisterRequest>()
        coEvery { stub.register(capture(request), any()) } returns Empty.getDefaultInstance()
        val service = ProjectionsService("my-store", stub, mockk<io.cratis.chronicle.readModels.ReadModelsService>(relaxed = true), "default")
        runBlocking { service.register(*projections) }
        return request.captured.projectionsList
    }

    private fun ProjectionsOuterClass.ProjectionDefinition.fromFor(eventType: KClass<*>): ProjectionsOuterClass.FromDefinition? =
        fromList.firstOrNull { it.key.id == eventType.simpleName }?.value

    private fun ProjectionsOuterClass.ProjectionDefinition.joinFor(eventType: KClass<*>): ProjectionsOuterClass.JoinDefinition? =
        joinList.firstOrNull { it.key.id == eventType.simpleName }?.value

    private fun ProjectionsOuterClass.ProjectionDefinition.isRemovedWithId(eventType: KClass<*>): Boolean =
        removedWithList.any { it.key.id == eventType.simpleName }

    // --- Model-bound: entering and mutual exclusion ---

    @Test
    fun `entering a variant keeps a From for the entering event`() {
        val definitions = register(MbVariantBacklogItem::class, MbVariantPullRequestItem::class)
        val backlog = definitions.first { it.readModel == "MbVariantBacklogItem" }
        assertEquals("\$eventSourceId", backlog.fromFor(MbVariantIssueCreated::class)!!.key)
    }

    @Test
    fun `a variant is removed with every sibling's entering event`() {
        val definitions = register(MbVariantBacklogItem::class, MbVariantPullRequestItem::class)
        val backlog = definitions.first { it.readModel == "MbVariantBacklogItem" }
        assertTrue(backlog.isRemovedWithId(MbVariantPullRequestCreated::class))
    }

    @Test
    fun `a variant is not removed with its own entering event`() {
        val definitions = register(MbVariantBacklogItem::class, MbVariantPullRequestItem::class)
        val backlog = definitions.first { it.readModel == "MbVariantBacklogItem" }
        assertFalse(backlog.isRemovedWithId(MbVariantIssueCreated::class))
    }

    @Test
    fun `mutual exclusion is symmetric across the group`() {
        val definitions = register(MbVariantBacklogItem::class, MbVariantPullRequestItem::class)
        val pullRequest = definitions.first { it.readModel == "MbVariantPullRequestItem" }
        assertTrue(pullRequest.isRemovedWithId(MbVariantIssueCreated::class))
    }

    // --- Model-bound: only the entering event can create ---

    @Test
    fun `a non-entering event is reclassified into a join rather than a From`() {
        val definitions = register(MbVariantBacklogItem::class, MbVariantPullRequestItem::class)
        val pullRequest = definitions.first { it.readModel == "MbVariantPullRequestItem" }
        assertEquals(null, pullRequest.fromFor(MbVariantBuildCompleted::class))
        assertEquals("id", pullRequest.joinFor(MbVariantBuildCompleted::class)!!.on)
    }

    // --- Model-bound: shared handlers ---

    @Test
    fun `a GlobalFor handler merges its mapping into every variant`() {
        val definitions = register(MbVariantBacklogItem::class, MbVariantPullRequestItem::class, MbVariantSharedHandlers::class)
        val backlog = definitions.first { it.readModel == "MbVariantBacklogItem" }
        assertEquals("id", backlog.joinFor(MbVariantTitleChanged::class)!!.on)
    }

    @Test
    fun `a GlobalFor handler is never registered as its own projection`() {
        val definitions = register(MbVariantBacklogItem::class, MbVariantPullRequestItem::class, MbVariantSharedHandlers::class)
        assertEquals(2, definitions.size)
    }

    @Test
    fun `a GlobalFor handler mapping a property the variant lacks throws`() {
        assertThrows(GlobalHandlerPropertyNotOnVariant::class.java) {
            register(MbVariantBacklogItem::class, MbVariantMismatchedGlobalHandler::class)
        }
    }

    // --- Model-bound: validation ---

    @Test
    fun `a variant with no EntersOn throws`() {
        assertThrows(VariantMustDeclareEntersOnEvent::class.java) {
            register(MbVariantMissingEntersOn::class)
        }
    }

    // --- Declarative: same semantics through the fluent builder ---

    @Test
    fun `the fluent builder reclassifies a non-entering From into a join on the declared key`() {
        val definitions = register(DecVariantBacklogItemProjection(), DecVariantPullRequestItemProjection())
        val pullRequest = definitions.first { it.readModel == "DecVariantPullRequestItem" }
        assertEquals(null, pullRequest.fromFor(MbVariantBuildCompleted::class))
        assertEquals("id", pullRequest.joinFor(MbVariantBuildCompleted::class)!!.on)
    }

    @Test
    fun `the fluent builder cross-wires mutual exclusion the same way`() {
        val definitions = register(DecVariantBacklogItemProjection(), DecVariantPullRequestItemProjection())
        val backlog = definitions.first { it.readModel == "DecVariantBacklogItem" }
        assertTrue(backlog.isRemovedWithId(MbVariantPullRequestCreated::class))
    }
}
