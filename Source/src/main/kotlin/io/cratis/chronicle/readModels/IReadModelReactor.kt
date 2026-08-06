// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.readModels

/**
 * Marks a class as a read model reactor - a type that reacts to read model instances being added,
 * modified or removed.
 *
 * This is a marker interface; dispatch is entirely by convention. A method named `added`, `modified`
 * or `removed` (matched case-insensitively, so `Added` works too) is invoked for the corresponding
 * [ReadModelChangeType]. Its first parameter is the read model - either a single instance or a
 * `List` of them - and that type decides which read model the reactor watches. An optional second
 * parameter of type [ReadModelChangeset] carries the key, sequence number and correlation id of the
 * change.
 *
 * A removal never carries an instance, so a Kotlin `removed` handler must declare its read model
 * parameter nullable. Like reactors, a handler may return an event (or a `List` of events) to be
 * appended as a side effect, using the changed instance's key as the event source id.
 *
 * ```kotlin
 * class EmployeeAlerts : IReadModelReactor {
 *     fun added(employee: EmployeeProfile) = EmployeeWelcomed(employee.name)
 *
 *     fun removed(employee: EmployeeProfile?, changeset: ReadModelChangeset<EmployeeProfile>) {
 *         println("${changeset.modelKey} left")
 *     }
 * }
 * ```
 *
 * Register an instance with [IReadModelReactors.register] to start watching.
 */
interface IReadModelReactor
