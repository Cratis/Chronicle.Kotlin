// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

/**
 * Narrows the events a reactor or reducer observes to those appended with this event source type.
 *
 * Set the type at append time through
 * [io.cratis.chronicle.eventSequences.AppendOptions.eventSourceType].
 *
 * When absent, the observer sees every event source type.
 *
 * @property value The event source type to observe.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class EventSourceType(val value: String)
