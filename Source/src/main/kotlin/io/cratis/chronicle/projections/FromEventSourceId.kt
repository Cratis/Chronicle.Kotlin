// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

/**
 * Sets a read model property to the event source id of every event the projection observes.
 *
 * A projection correlates events to a read model instance by key, and the sink stores each instance
 * under its own `id` - but nothing carries the key into the read model on its own. Leave the `id`
 * unmapped and every instance the projection produces lands on the same empty key, silently sharing
 * one document:
 *
 * ```kotlin
 * @ReadModel
 * @FromEvent(EmployeeHired::class)
 * data class EmployeeDetails(
 *     @FromEventSourceId val id: String = "",
 *     val firstName: String = ""
 * )
 * ```
 *
 * This is the model-bound form of the fluent builder's
 * [IAllSetBuilderFor.toEventSourceId]. [FromEvery] cannot express it: its `contextProperty` maps
 * from a named [io.cratis.chronicle.events.EventContext] property, and the event source id is the
 * key the projection is partitioned by rather than a value read off the context.
 *
 * **The kernel does not honor this yet.** `${'$'}eventSourceId` resolves to nothing in a property
 * mapping, in either the per-event or the every-event position, so the fluent form has the same
 * problem - see https://github.com/Cratis/Chronicle/issues/3924. This annotation says what the
 * projection means; it starts taking effect when that is fixed.
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class FromEventSourceId
