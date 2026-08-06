// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

/**
 * The kernel-side filters narrowing which events an observer is delivered.
 *
 * Shared by reactors and reducers - the annotations and their defaults are identical for both.
 *
 * @property filterTags Events must carry all of these tags. Empty means no tag filtering.
 * @property eventSourceType The event source type to observe. Empty means every source type.
 * @property eventStreamType The event stream type to observe. `All` means every stream type.
 */
internal data class ObserverFilters(
    val filterTags: List<String>,
    val eventSourceType: String,
    val eventStreamType: String
) {
    /** Whether anything is actually being filtered. */
    val isUnfiltered: Boolean
        get() = filterTags.isEmpty() &&
            eventSourceType == UNSPECIFIED_EVENT_SOURCE_TYPE &&
            eventStreamType == ALL_EVENT_STREAM_TYPES

    companion object {
        /** The event source type meaning "every source type", matching the C# client. */
        const val UNSPECIFIED_EVENT_SOURCE_TYPE = ""

        /** The event stream type meaning "every stream type", matching the C# client. */
        const val ALL_EVENT_STREAM_TYPES = "All"

        /** Filters nothing out. */
        val none = ObserverFilters(emptyList(), UNSPECIFIED_EVENT_SOURCE_TYPE, ALL_EVENT_STREAM_TYPES)

        /**
         * Reads the filter annotations off [observerClass].
         *
         * Repeated annotations are read through `getAnnotationsByType`, which unwraps the JVM
         * container - so it sees them whether the observer was written in Kotlin or Java.
         */
        fun from(observerClass: KClass<*>): ObserverFilters = ObserverFilters(
            filterTags = observerClass.java.getAnnotationsByType(FilterEventsByTag::class.java)
                .map { it.value }
                .distinct(),
            eventSourceType = observerClass.findAnnotation<EventSourceType>()?.value
                ?: UNSPECIFIED_EVENT_SOURCE_TYPE,
            eventStreamType = observerClass.findAnnotation<EventStreamType>()?.value
                ?: ALL_EVENT_STREAM_TYPES
        )

        /** Reads the descriptive [Tag] labels off [observerClass]. */
        fun tagsOf(observerClass: KClass<*>): List<String> =
            observerClass.java.getAnnotationsByType(Tag::class.java)
                .flatMap { it.value.asIterable() }
                .distinct()
    }
}
