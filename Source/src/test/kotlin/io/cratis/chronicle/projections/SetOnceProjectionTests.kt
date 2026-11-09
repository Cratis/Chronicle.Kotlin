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
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@EventType
private data class AuthorNoted(val author: String, val category: String)

private data class Article(
    val id: String = "",
    val firstAuthor: String = "",
    val firstSeen: String = "",
    val firstAuthorByCategory: Map<String, String> = emptyMap(),
    val notes: List<ArticleNote> = emptyList(),
)

private data class ArticleNote(val author: String = "")

@ReadModel
@FromEvent(AuthorNoted::class)
private data class ModelBoundArticle(
    val id: String = "",
    @SetFrom("author", AuthorNoted::class, setOnce = true)
    val firstAuthor: String = "",
    @SetFromContext(AuthorNoted::class, "occurred", setOnce = true)
    val firstSeen: String = "",
    @SetFrom("author", AuthorNoted::class)
    val lastAuthor: String = "",
)

class SetOnceProjectionTests {

    private fun fromMappings(configure: (IFromBuilderFor<Article, AuthorNoted>) -> Unit): Map<String, String> {
        val builder = ProjectionBuilderFor(Article::class)
        builder.from(AuthorNoted::class, configure)
        return builder.fromEntries.single().properties
    }

    @Test
    fun `set once wraps an event property in the set once expression`() {
        assertEquals(mapOf("firstAuthor" to "\$setOnce(author)"), fromMappings { it.setOnce(Article::firstAuthor).toProperty("author") })
    }

    @Test
    fun `set once wraps every kind of value expression`() {
        val mappings = fromMappings {
            it.setOnce<String>("firstSeen").toEventContextProperty("occurred")
            it.setOnce(Article::id).toEventSourceId()
            it.setOnce(Article::firstAuthor).toCausedBy(CausedByProperty.SUBJECT)
        }
        assertEquals("\$setOnce(\$eventContext(occurred))", mappings["firstSeen"])
        assertEquals("\$setOnce(\$eventSourceId)", mappings["id"])
        assertEquals("\$setOnce(\$causedBy(subject))", mappings["firstAuthor"])
    }

    @Test
    fun `set once applies to a dictionary entry`() {
        val mappings = fromMappings {
            it.setOnce(Article::firstAuthorByCategory, DictionaryKey.fromEvent("category")).toProperty("author")
        }
        assertEquals(mapOf("firstAuthorByCategory.\$eventContent.category" to "\$setOnce(author)"), mappings)
    }

    @Test
    fun `set once applies to a dictionary entry by property name`() {
        val mappings = fromMappings {
            it.setOnce<String>("firstAuthorByCategory", DictionaryKey.fromEvent("category")).toProperty("author")
        }
        assertEquals(mapOf("firstAuthorByCategory.\$eventContent.category" to "\$setOnce(author)"), mappings)
    }

    @Test
    fun `plain set is left unwrapped`() {
        assertEquals(mapOf("firstAuthor" to "author"), fromMappings { it.set(Article::firstAuthor).toProperty("author") })
    }

    @Test
    fun `set once is available for every event`() {
        val builder = ProjectionBuilderFor(Article::class)
        builder.fromEvery { it.setOnce(Article::firstSeen).toEventContextProperty("occurred") }
        assertEquals(mapOf("firstSeen" to "\$setOnce(\$eventContext(occurred))"), builder.fromEveryProperties)
    }

    @Test
    fun `set once is available on a join`() {
        val builder = ProjectionBuilderFor(Article::class)
        builder.join(AuthorNoted::class) { it.on(Article::id).setOnce(Article::firstAuthor).toProperty("author") }
        assertEquals(mapOf("firstAuthor" to "\$setOnce(author)"), builder.joinEntries.single().properties)
    }

    @Test
    fun `set once is available on a child`() {
        val builder = ProjectionBuilderFor(Article::class)
        builder.children(Article::notes, ArticleNote::class) { children ->
            children.from(AuthorNoted::class) { it.setOnce<String>("author").toProperty("author") }
        }
        assertEquals(mapOf("author" to "\$setOnce(author)"), builder.childrenEntries.single().fromEntries.single().properties)
    }

    @Test
    fun `model-bound set once wraps the event and context mappings`() {
        val stub = mockk<ProjectionsGrpcKt.ProjectionsCoroutineStub>()
        val request = slot<ProjectionsOuterClass.RegisterRequest>()
        coEvery { stub.register(capture(request), any()) } returns Empty.getDefaultInstance()
        val service = ProjectionsService("my-store", stub, mockk<io.cratis.chronicle.readModels.ReadModelsService>(relaxed = true), "default")
        runBlocking { service.register(ModelBoundArticle::class) }

        val properties = request.captured.projectionsList.single()
            .fromList.first { it.key.id == AuthorNoted::class.simpleName }.value.propertiesMap
        assertEquals("\$setOnce(author)", properties["firstAuthor"])
        assertEquals("\$setOnce(\$eventContext(occurred))", properties["firstSeen"])
        assertEquals("author", properties["lastAuthor"])
    }
}
