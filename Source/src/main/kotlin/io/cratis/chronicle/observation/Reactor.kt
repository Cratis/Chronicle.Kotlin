// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

/**
 * Marks a class as a Chronicle reactor.
 *
 * @property id Explicit identifier. Defaults to the class simple name.
 * @property eventSequence The identifier of the event sequence to observe.
 *   Defaults to the event log when not specified.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Reactor(val id: String = "", val eventSequence: String = "")
