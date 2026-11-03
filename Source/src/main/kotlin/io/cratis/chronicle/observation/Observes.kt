// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import kotlin.reflect.KClass

/**
 * Declares an event type a batch method observes.
 *
 * A batch method is a reactor or reducer method whose first parameter is an [EventBatch]. It is
 * called with a whole batch of events rather than one at a time, and this annotation - repeated once
 * per event type - says which events go into that batch. Inside, the method pattern-matches on the
 * events (`when (val event = it.event) { is ... }` in Kotlin, `switch` or `instanceof` in Java).
 *
 * Each event type reaches exactly one method of an observer. Observing an event type that a
 * per-event handler of the same observer also takes, or that another batch method observes, is
 * refused at registration, and so is this annotation on a method that does not take an [EventBatch].
 *
 * ```kotlin
 * @Observes(UserRegistered::class)
 * @Observes(UserRenamed::class)
 * suspend fun index(batch: EventBatch, context: BatchContext) { ... }
 * ```
 *
 * ```java
 * @Observes(UserRegistered.class)
 * @Observes(UserRenamed.class)
 * public void index(EventBatch batch, BatchContext context) { ... }
 * ```
 *
 * @property value The class of the event type observed, which must be annotated with
 *   [io.cratis.chronicle.events.EventType]. It is named `value` so Java can write it positionally.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
@MustBeDocumented
annotation class Observes(val value: KClass<*>)
