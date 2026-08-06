// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

/**
 * Narrows the events a reactor or reducer observes to those appended with this tag.
 *
 * The filter is applied by the kernel, so a filtered-out event is never delivered - unlike checking
 * the tag inside the handler, which still pays for the delivery.
 *
 * Repeatable. Tag an event at append time through
 * [io.cratis.chronicle.eventSequences.AppendOptions.tags].
 *
 * Use [Tag] instead to label the observer itself without changing what it observes.
 *
 * @property value The tag an event must carry to be observed.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@JvmRepeatable(FilterEventsByTags::class)
annotation class FilterEventsByTag(val value: String)
