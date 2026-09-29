// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.constraints;

import io.cratis.chronicle.java.UniqueConstraintBuilderJavaBridge;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A Java caller making several properties unique together, written the way a reader would write it.
 *
 * Java has no property references, so it names the properties and goes through
 * {@link UniqueConstraintBuilderJavaBridge}.
 */
class JavaUniqueConstraintUsageTest {

    /** An event with a name in two parts. */
    public record PersonRegistered(String firstName, String lastName) {
    }

    /** Another event carrying the same name. */
    public record PersonRenamed(String firstName, String lastName) {
    }

    @Test
    void several_property_names_are_unique_together() {
        var builder = new ConstraintBuilder();

        builder.unique(unique -> {
            UniqueConstraintBuilderJavaBridge.on(unique, PersonRegistered.class, "firstName", "lastName");
            unique.ignoreCasing();
        });

        var entry = (ConstraintBuilderEntry.UniqueEntry) builder.build().get(0);
        assertEquals(
            List.of(new UniqueEventDefinition(kotlin.jvm.JvmClassMappingKt.getKotlinClass(PersonRegistered.class), List.of("firstName", "lastName"))),
            entry.getEventDefinitions());
        assertTrue(entry.getIgnoreCasing());
    }

    @Test
    void one_property_name_still_works() {
        var builder = new ConstraintBuilder();

        builder.unique(unique -> {
            UniqueConstraintBuilderJavaBridge.on(unique, PersonRegistered.class, "lastName");
        });

        var entry = (ConstraintBuilderEntry.UniqueEntry) builder.build().get(0);
        assertEquals(List.of("lastName"), entry.getEventDefinitions().get(0).getProperties());
    }

    @Test
    void a_constraint_spans_several_event_types() {
        var builder = new ConstraintBuilder();

        builder.unique(unique -> {
            UniqueConstraintBuilderJavaBridge.on(unique, PersonRegistered.class, "firstName", "lastName");
            UniqueConstraintBuilderJavaBridge.on(unique, PersonRenamed.class, "firstName", "lastName");
        });

        var entry = (ConstraintBuilderEntry.UniqueEntry) builder.build().get(0);
        assertEquals(2, entry.getEventDefinitions().size());
    }

    @Test
    void the_same_event_type_twice_is_rejected() {
        var builder = new ConstraintBuilder();

        assertThrows(EventTypeAlreadyAddedToUniqueConstraint.class, () -> builder.unique(unique -> {
            UniqueConstraintBuilderJavaBridge.on(unique, PersonRegistered.class, "firstName");
            UniqueConstraintBuilderJavaBridge.on(unique, PersonRegistered.class, "lastName");
        }));
    }
}
