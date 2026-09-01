// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.events.migrations;

import io.cratis.chronicle.events.EventType;

/**
 * An event type migration, written the way Java writes one - {@code Class} references to
 * {@link EventTypeMigration}'s constructor instead of {@link kotlin.reflect.KClass}, and property
 * names to {@link EventTypeMigrationBuilder} instead of {@link kotlin.reflect.KProperty1}. Neither
 * was reachable from Java before: the base class constructor only accepted a {@code KClass}, and the
 * builder's {@code renamedFrom}/{@code defaultValue}/{@code split}/{@code combine} only accepted a
 * {@code KProperty1} - both un-producible from Java.
 *
 * Nothing here runs - compiling it is the assertion.
 */
public final class JavaEventTypeMigrationUsage {

    private JavaEventTypeMigrationUsage() {
    }

    /** The previous generation of the event, as it was stored. */
    @EventType(generation = 1)
    public record JavaMigrationPersonV1(String name) {
    }

    /** The upgraded generation - the name has been split, and a new required field added. */
    @EventType(id = "JavaMigrationPersonV1", generation = 2)
    public record JavaMigrationPersonV2(String fullName, String country) {
    }

    /** The migration between the two generations above, discovered by its public no-argument constructor. */
    public static class JavaPersonMigration extends EventTypeMigration<JavaMigrationPersonV2, JavaMigrationPersonV1> {
        public JavaPersonMigration() {
            super(JavaMigrationPersonV2.class, JavaMigrationPersonV1.class);
        }

        @Override
        public void upcast(EventTypeMigrationBuilder<JavaMigrationPersonV2, JavaMigrationPersonV1> builder) {
            builder.renamedFrom("fullName", "name").defaultValue("country", "unknown");
        }

        @Override
        public void downcast(EventTypeMigrationBuilder<JavaMigrationPersonV1, JavaMigrationPersonV2> builder) {
            builder.renamedFrom("name", "fullName");
        }
    }

    /** {@code split} and {@code combine}, exercised directly on the builder the way a spec would. */
    public static String splitAndCombine() {
        var splitting = new EventTypeMigrationBuilder<JavaMigrationPersonV2, JavaMigrationPersonV1>();
        splitting.split("fullName", "name", " ", 0);

        var combining = new EventTypeMigrationBuilder<JavaMigrationPersonV2, JavaMigrationPersonV1>();
        combining.combine("fullName", " ", "name");

        return combining.toJson();
    }
}
