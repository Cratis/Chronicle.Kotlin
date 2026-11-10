// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import Cratis.Chronicle.Contracts.Projections.ProjectionsGrpcKt
import Cratis.Chronicle.Contracts.Projections.ProjectionsOuterClass
import com.google.protobuf.Empty
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.readModels.ReadModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlin.reflect.full.findAnnotations
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

@EventType
private data class ThreadStarted(val title: String)

@EventType
private data class CommentPosted(val commentId: String, val text: String, val postedAt: Long)

@EventType
private data class CommentPinned(val commentId: String, val text: String, val postedAt: Long)

private data class Comment(val commentId: String = "", val text: String = "", val postedAt: Long = 0)

private data class CommentThread(val id: String = "", val comments: List<Comment> = emptyList())

@ReadModel
@FromEvent(ThreadStarted::class)
private data class UnorderedThread(
    @ChildrenFrom(CommentPosted::class, key = "commentId", identifiedBy = "commentId")
    val comments: List<Comment> = emptyList(),
)

@ReadModel
@FromEvent(ThreadStarted::class)
private data class NewestFirstThread(
    @ChildrenFrom(CommentPosted::class, key = "commentId", identifiedBy = "commentId", prepend = true)
    val comments: List<Comment> = emptyList(),
)

@ReadModel
@FromEvent(ThreadStarted::class)
private data class SameOrderingThread(
    @ChildrenFrom(CommentPosted::class, key = "commentId", identifiedBy = "commentId", orderByDescending = "postedAt")
    @ChildrenFrom(CommentPinned::class, key = "commentId", identifiedBy = "commentId", orderByDescending = "postedAt")
    val comments: List<Comment> = emptyList(),
)

@ReadModel
@FromEvent(ThreadStarted::class)
private data class DifferentOrderingThread(
    @ChildrenFrom(CommentPosted::class, key = "commentId", identifiedBy = "commentId", orderBy = "postedAt")
    @ChildrenFrom(CommentPinned::class, key = "commentId", identifiedBy = "commentId", prepend = true)
    val comments: List<Comment> = emptyList(),
)

@ReadModel
@FromEvent(ThreadStarted::class)
private data class TwoOrderingsThread(
    @ChildrenFrom(CommentPosted::class, key = "commentId", identifiedBy = "commentId", prepend = true, orderBy = "postedAt")
    val comments: List<Comment> = emptyList(),
)

@ReadModel
@FromEvent(ThreadStarted::class)
private data class UnknownSortThread(
    @ChildrenFrom(CommentPosted::class, key = "commentId", identifiedBy = "commentId", orderBy = "missing")
    val comments: List<Comment> = emptyList(),
)

private class OrderedThreadProjection : IProjectionFor<CommentThread> {
    override fun define(builder: IProjectionBuilderFor<CommentThread>) {
        builder.children(CommentThread::comments, Comment::class) { children ->
            children.identifiedBy("commentId").orderBy(Comment::postedAt).from(CommentPosted::class) { it.usingKey("commentId") }
        }
    }
}

class ChildrenOrderingProjectionTests {

    private fun orderingOf(configure: (IChildrenBuilderFor<Comment>) -> Unit): ChildrenOrdering? {
        val builder = ProjectionBuilderFor(CommentThread::class)
        builder.children(CommentThread::comments, Comment::class, configure)
        return builder.childrenOrderings[builder.childrenEntries.single().propertyName]
    }

    private fun register(projection: Any): List<ProjectionsOuterClass.ProjectionDefinition> {
        val stub = mockk<ProjectionsGrpcKt.ProjectionsCoroutineStub>()
        val captured = mutableListOf<ProjectionsOuterClass.RegisterRequest>()
        coEvery { stub.register(capture(captured), any()) } returns Empty.getDefaultInstance()
        val service = ProjectionsService("my-store", stub, mockk<io.cratis.chronicle.readModels.ReadModelsService>(relaxed = true), "default")
        runBlocking { service.register(projection) }
        return captured.single().projectionsList
    }

    @Test
    fun `children have no ordering unless one is declared`() {
        assertNull(orderingOf { it.identifiedBy("commentId") })
    }

    @Test
    fun `prepend places new children first`() {
        assertEquals(ChildrenOrdering(ChildrenPlacement.PREPEND), orderingOf { it.prepend() })
    }

    @Test
    fun `order by sorts ascending by the child property`() {
        assertEquals(
            ChildrenOrdering(ChildrenPlacement.SORTED, "postedAt", SortDirection.ASCENDING),
            orderingOf { it.orderBy(Comment::postedAt) },
        )
    }

    @Test
    fun `order by descending sorts descending by the child property name`() {
        assertEquals(
            ChildrenOrdering(ChildrenPlacement.SORTED, "postedAt", SortDirection.DESCENDING),
            orderingOf { it.orderByDescending("postedAt") },
        )
    }

    @Test
    fun `a second ordering is refused`() {
        assertThrows(ChildrenOrderingAlreadyDefined::class.java) {
            orderingOf { it.prepend().orderByDescending(Comment::postedAt) }
        }
    }

    @Test
    fun `sorting by an unknown child property is refused`() {
        assertThrows(UnknownReadModelProperty::class.java) { orderingOf { it.orderBy("missing") } }
    }

    @Test
    fun `model-bound children declare their ordering on the annotation`() {
        val annotations = NewestFirstThread::comments.findAnnotations<ChildrenFrom>()
        assertEquals(ChildrenOrdering.prepend(), resolveChildrenOrdering("comments", Comment::class, annotations))
    }

    @Test
    fun `model-bound children may repeat the same ordering on every annotation`() {
        val annotations = SameOrderingThread::comments.findAnnotations<ChildrenFrom>()
        assertEquals(
            ChildrenOrdering.sorted("postedAt", SortDirection.DESCENDING),
            resolveChildrenOrdering("comments", Comment::class, annotations),
        )
    }

    @Test
    fun `model-bound children with different orderings are refused`() {
        assertThrows(ConflictingChildrenOrdering::class.java) { register(DifferentOrderingThread::class) }
    }

    @Test
    fun `a model-bound annotation declaring two orderings is refused`() {
        assertThrows(ConflictingChildrenOrdering::class.java) { register(TwoOrderingsThread::class) }
    }

    @Test
    fun `a model-bound ordering by an unknown child property is refused`() {
        assertThrows(UnknownReadModelProperty::class.java) { register(UnknownSortThread::class) }
    }

    @Test
    fun `unordered children still register`() {
        val children = register(UnorderedThread::class).single().childrenMap.getValue("comments")
        assertEquals("commentId", children.identifiedBy)
    }

    @Test
    fun `registering a fluent ordering sends it with the children`() {
        val ordering = register(OrderedThreadProjection()).single().childrenMap.getValue("comments").ordering
        assertEquals(ProjectionsOuterClass.ChildrenPlacement.Sorted, ordering.placement)
        assertEquals("postedAt", ordering.sortBy)
        assertEquals(ProjectionsOuterClass.SortDirection.Ascending, ordering.direction)
    }

    @Test
    fun `registering a model-bound ordering sends it with the children`() {
        val ordering = register(NewestFirstThread::class).single().childrenMap.getValue("comments").ordering
        assertEquals(ProjectionsOuterClass.ChildrenPlacement.Prepend, ordering.placement)
        assertEquals("", ordering.sortBy)
    }

    @Test
    fun `registering a model-bound descending ordering sends its direction`() {
        val ordering = register(SameOrderingThread::class).single().childrenMap.getValue("comments").ordering
        assertEquals(ProjectionsOuterClass.ChildrenPlacement.Sorted, ordering.placement)
        assertEquals("postedAt", ordering.sortBy)
        assertEquals(ProjectionsOuterClass.SortDirection.Descending, ordering.direction)
    }

    @Test
    fun `unordered children send no ordering`() {
        val children = register(UnorderedThread::class).single().childrenMap.getValue("comments")
        assertEquals(false, children.hasOrdering())
    }
}
