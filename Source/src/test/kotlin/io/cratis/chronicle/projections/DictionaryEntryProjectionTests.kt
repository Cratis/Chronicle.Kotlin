// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import io.cratis.chronicle.events.EventType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

@EventType
private data class ItemCategorized(val itemId: String, val category: String, val amount: Long, val label: String)

private data class CategorySummary(
    val id: String = "",
    val countByCategory: Map<String, Long> = emptyMap(),
    val totalByCategory: MutableMap<String, Long> = mutableMapOf(),
    val labelByCategory: Map<String, String>? = null,
    val labelByAuthor: Map<String, String> = emptyMap(),
    val name: String = "",
)

class DictionaryEntryProjectionTests {

    private fun mappingsFor(configure: (IFromBuilderFor<CategorySummary, ItemCategorized>) -> Unit): Map<String, String> {
        val builder = ProjectionBuilderFor(CategorySummary::class)
        builder.from(ItemCategorized::class, configure)
        return builder.fromEntries.single().properties
    }

    @Test
    fun `a key from the event content addresses the entry with the event content marker`() {
        val mappings = mappingsFor { it.count(CategorySummary::countByCategory, DictionaryKey.fromEvent("category")) }
        assertEquals(mapOf("countByCategory.\$eventContent.category" to "\$count"), mappings)
    }

    @Test
    fun `a key from the event context addresses the entry with the event context marker`() {
        val mappings = mappingsFor { it.increment(CategorySummary::countByCategory, DictionaryKey.fromContext("eventType.id")) }
        assertEquals(mapOf("countByCategory.\$eventContext.eventType.id" to "\$increment"), mappings)
    }

    @Test
    fun `a key from who caused the event addresses the entry with the caused by marker`() {
        val mappings = mappingsFor {
            it.set(CategorySummary::labelByAuthor, DictionaryKey.fromCausedBy(CausedByProperty.USER_NAME)).toProperty("label")
        }
        assertEquals(mapOf("labelByAuthor.\$causedBy.userName" to "label"), mappings)
    }

    @Test
    fun `every arithmetic operation writes its expression into the keyed entry`() {
        val key = DictionaryKey.fromEvent("category")
        val mappings = mappingsFor {
            it.decrement("countByCategory", key)
            it.add(CategorySummary::totalByCategory, key).with("amount")
        }
        assertEquals("\$decrement", mappings["countByCategory.\$eventContent.category"])
        assertEquals("\$add(amount)", mappings["totalByCategory.\$eventContent.category"])

        val subtracted = mappingsFor { it.subtract("totalByCategory", key).with("amount") }
        assertEquals(mapOf("totalByCategory.\$eventContent.category" to "\$subtract(amount)"), subtracted)
    }

    @Test
    fun `setting an entry by property name maps the event value into the keyed entry`() {
        val mappings = mappingsFor { it.set<String>("labelByCategory", DictionaryKey.fromEvent("category")).toProperty("label") }
        assertEquals(mapOf("labelByCategory.\$eventContent.category" to "label"), mappings)
    }

    @Test
    fun `removing a key writes the remove expression for the keyed entry`() {
        val mappings = mappingsFor { it.removeKey(CategorySummary::countByCategory, DictionaryKey.fromEvent("category")) }
        assertEquals(mapOf("countByCategory.\$eventContent.category" to "\$remove"), mappings)
    }

    @Test
    fun `removing a key by property name writes the remove expression`() {
        val mappings = mappingsFor { it.removeKey("countByCategory", DictionaryKey.fromContext("eventSourceId")) }
        assertEquals(mapOf("countByCategory.\$eventContext.eventSourceId" to "\$remove"), mappings)
    }

    @Test
    fun `an entry on a property that is not a map is refused`() {
        assertThrows(IllegalArgumentException::class.java) {
            mappingsFor { it.count("name", DictionaryKey.fromEvent("category")) }
        }
    }

    @Test
    fun `an entry on an unknown property is refused`() {
        assertThrows(UnknownReadModelProperty::class.java) {
            mappingsFor { it.removeKey("missing", DictionaryKey.fromEvent("category")) }
        }
    }

    @Test
    fun `a key without a property path is refused`() {
        assertThrows(IllegalArgumentException::class.java) { DictionaryKey.fromEvent(" ") }
        assertThrows(IllegalArgumentException::class.java) { DictionaryKey.fromContext("") }
    }

    @Test
    fun `the caused by sugar maps each identity property to the kernel expression`() {
        val mappings = mappingsFor {
            it.set(CategorySummary::name).toCausedBy(CausedByProperty.SUBJECT)
            it.set(CategorySummary::id).toCausedBy(CausedByProperty.NAME)
        }
        assertEquals("\$causedBy(subject)", mappings["name"])
        assertEquals("\$causedBy(name)", mappings["id"])
    }

    @Test
    fun `the caused by sugar is available for every event`() {
        val builder = ProjectionBuilderFor(CategorySummary::class)
        builder.fromEvery { it.set(CategorySummary::name).toCausedBy(CausedByProperty.USER_NAME) }
        assertEquals(mapOf("name" to "\$causedBy(userName)"), builder.fromEveryProperties)
    }
}
