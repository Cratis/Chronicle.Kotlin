// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

/**
 * Narrows the events a reactor or reducer observes to those appended to this event stream type.
 *
 * Set the type at append time through
 * [io.cratis.chronicle.eventSequences.AppendOptions.eventStreamType].
 *
 * When absent, the observer sees every event stream type.
 *
 * @property value The event stream type to observe.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class EventStreamType(val value: String)
