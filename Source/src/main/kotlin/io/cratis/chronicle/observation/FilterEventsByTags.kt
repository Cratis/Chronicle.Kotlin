// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

/**
 * Holds repeated [FilterEventsByTag] annotations.
 *
 * Declared so that repeating [FilterEventsByTag] works from Java as well as Kotlin. Apply
 * [FilterEventsByTag] directly rather than using this.
 *
 * @property value The repeated [FilterEventsByTag] annotations.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class FilterEventsByTags(vararg val value: FilterEventsByTag)
