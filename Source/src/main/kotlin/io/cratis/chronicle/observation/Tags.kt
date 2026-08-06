// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

/**
 * Holds repeated [Tag] annotations.
 *
 * Declared so that repeating [Tag] works from Java as well as Kotlin. Apply [Tag] directly rather
 * than using this.
 *
 * @property value The repeated [Tag] annotations.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Tags(vararg val value: Tag)
