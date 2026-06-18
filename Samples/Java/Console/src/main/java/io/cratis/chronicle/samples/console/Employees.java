// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console;

import java.util.Arrays;
import java.util.List;

/** The employees the sample operates on, shared between seeding and the interactive console. */
public class Employees {
    public static final List<Person> employees = Arrays.asList(
        new Person("a0000001-0000-0000-0000-000000000000", "Ada", "Lovelace"),
        new Person("a0000002-0000-0000-0000-000000000000", "Grace", "Hopper"),
        new Person("a0000003-0000-0000-0000-000000000000", "Alan", "Turing")
    );

    /** Builds the canonical, unique email address for a person (e.g. ada.lovelace@cratis.io). */
    public static String emailFor(Person person) {
        return (person.getFirstName() + "." + person.getLastName() + "@cratis.io").toLowerCase();
    }
}
