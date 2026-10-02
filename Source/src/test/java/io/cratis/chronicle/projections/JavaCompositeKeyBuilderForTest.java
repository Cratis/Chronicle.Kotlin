// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections;

import kotlin.jvm.JvmClassMappingKt;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaCompositeKeyBuilderForTest {
    @Test
    void compositeKeyDatePartsAreCallableFromJava() {
        var builder = new ProjectionBuilderFor<>(JvmClassMappingKt.getKotlinClass(WeeklyKey.class));
        builder.from(SomeEvent.class, from -> from.usingCompositeKey(key -> {
            key.toEventContextProperty("Year", "Occurred.Year")
                .toEventContextProperty("Week", "Occurred.Week");
        }));
        assertEquals(
            "$composite(Year=$eventContext(Occurred.Year),Week=$eventContext(Occurred.Week))",
            builder.getFromEntries().get(0).getKey()
        );
    }

    @Test
    void customBuilderInheritsEventContextDefaultWithoutOverridingIt() {
        var custom = new LegacyCompositeKeyBuilder();
        custom.toEventContextProperty("Year", "Occurred.Year")
            .toEventContextProperty("Week", "Occurred.Week");
        assertEquals(
            "$composite(Year=$eventContext(Occurred.Year),Week=$eventContext(Occurred.Week))",
            custom.stored.build()
        );
    }

    private static final class LegacyCompositeKeyBuilder implements ICompositeKeyBuilderFor {
        private final CompositeKeyBuilderFor stored = new CompositeKeyBuilderFor();

        @Override
        public ICompositeKeyBuilderFor property(String targetPropertyName, String eventPropertyName) {
            stored.property(targetPropertyName, eventPropertyName);
            return this;
        }
    }

    record WeeklyKey(int year, int week) { }
    record SomeEvent(String name) { }
}
