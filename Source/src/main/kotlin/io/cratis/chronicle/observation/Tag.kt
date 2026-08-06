// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

/**
 * Labels a reactor or reducer with one or more descriptive tags.
 *
 * Tags describe the observer itself - what it is for, who owns it, which subsystem it belongs to -
 * and show up alongside it in tooling. They do not affect which events it observes; use
 * [FilterEventsByTag] for that.
 *
 * Repeatable, and each use takes any number of tags, so all of these are equivalent:
 *
 * ```kotlin
 * @Tag("analytics", "reporting")
 *
 * @Tag("analytics")
 * @Tag("reporting")
 * ```
 *
 * The element is named `value` and the repeatable container is declared explicitly, because Kotlin's
 * own `@Repeatable` produces a container Java cannot use, and Java's shorthand `@Tag({"a", "b"})`
 * only works for an element called `value`.
 *
 * @property value The tags to label the observer with.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@JvmRepeatable(Tags::class)
annotation class Tag(vararg val value: String)
