// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ObserverFiltersTests {

    @Reactor
    class Unannotated {
        fun bookReturned(@Suppress("UNUSED_PARAMETER") event: BookReturned) = Unit
    }

    @Reactor
    @Tag("analytics", "reporting")
    class MultipleTagsInOneAnnotation {
        fun bookReturned(@Suppress("UNUSED_PARAMETER") event: BookReturned) = Unit
    }

    @Reactor
    @Tag("analytics")
    @Tag("reporting")
    class RepeatedTagAnnotations {
        fun bookReturned(@Suppress("UNUSED_PARAMETER") event: BookReturned) = Unit
    }

    @Reactor
    @Tag("analytics")
    @Tag("analytics", "reporting")
    class DuplicateTags {
        fun bookReturned(@Suppress("UNUSED_PARAMETER") event: BookReturned) = Unit
    }

    @Reactor
    @FilterEventsByTag("critical")
    @FilterEventsByTag("production")
    class FilteredByTags {
        fun bookReturned(@Suppress("UNUSED_PARAMETER") event: BookReturned) = Unit
    }

    @Reactor
    @EventSourceType("Patient")
    @EventStreamType("Onboarding")
    class FilteredByTypes {
        fun bookReturned(@Suppress("UNUSED_PARAMETER") event: BookReturned) = Unit
    }

    @Reducer
    @Tag("reporting")
    @FilterEventsByTag("critical")
    class TaggedReducer {
        fun borrowed(event: BookBorrowed, state: BookState?) = BookState(event.title, true)
    }

    @Test
    fun `an unannotated observer filters nothing`() {
        val filters = ObserverFilters.from(Unannotated::class)
        assertTrue(filters.isUnfiltered)
        assertTrue(filters.filterTags.isEmpty())
        assertEquals("", filters.eventSourceType)
        assertEquals("All", filters.eventStreamType)
    }

    @Test
    fun `an unannotated observer has no tags`() {
        assertTrue(ObserverFilters.tagsOf(Unannotated::class).isEmpty())
    }

    @Test
    fun `several tags in one annotation are read`() {
        assertEquals(listOf("analytics", "reporting"), ObserverFilters.tagsOf(MultipleTagsInOneAnnotation::class))
    }

    @Test
    fun `repeated tag annotations are read`() {
        assertEquals(listOf("analytics", "reporting"), ObserverFilters.tagsOf(RepeatedTagAnnotations::class))
    }

    @Test
    fun `duplicate tags are collapsed`() {
        assertEquals(listOf("analytics", "reporting"), ObserverFilters.tagsOf(DuplicateTags::class))
    }

    @Test
    fun `filter tags are read`() {
        val filters = ObserverFilters.from(FilteredByTags::class)
        assertEquals(listOf("critical", "production"), filters.filterTags)
        assertFalse(filters.isUnfiltered)
    }

    @Test
    fun `labelling tags do not become filters`() {
        // @Tag describes the observer; only @FilterEventsByTag narrows what it observes.
        assertTrue(ObserverFilters.from(RepeatedTagAnnotations::class).isUnfiltered)
    }

    @Test
    fun `event source and stream types are read`() {
        val filters = ObserverFilters.from(FilteredByTypes::class)
        assertEquals("Patient", filters.eventSourceType)
        assertEquals("Onboarding", filters.eventStreamType)
        assertFalse(filters.isUnfiltered)
    }

    @Test
    fun `a reactor registration carries its tags and filters`() {
        val registration = ReactorRegistration.from(FilteredByTags::class)
        assertEquals(listOf("critical", "production"), registration.filters.filterTags)
    }

    @Test
    fun `a reducer registration carries its tags and filters`() {
        val registration = ReducerRegistration.from(TaggedReducer::class)
        assertEquals(listOf("reporting"), registration.tags)
        assertEquals(listOf("critical"), registration.filters.filterTags)
    }
}
