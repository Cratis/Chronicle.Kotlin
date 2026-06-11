// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console

/** A person that can be hired as an employee. */
data class Person(val id: String, val firstName: String, val lastName: String)

/** The employees the sample operates on, shared between seeding and the interactive console. */
val employees = listOf(
    Person("a0000001-0000-0000-0000-000000000000", "Ada",   "Lovelace"),
    Person("a0000002-0000-0000-0000-000000000000", "Grace", "Hopper"),
    Person("a0000003-0000-0000-0000-000000000000", "Alan",  "Turing")
)

/** Builds the canonical, unique email address for a person (e.g. ada.lovelace@cratis.io). */
fun emailFor(person: Person): String =
    "${person.firstName}.${person.lastName}@cratis.io".lowercase()
