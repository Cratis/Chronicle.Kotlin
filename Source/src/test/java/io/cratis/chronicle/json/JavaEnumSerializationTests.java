// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.json;

import io.cratis.chronicle.events.EventType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaEnumSerializationTests {
    private enum Status { Pending, Dispatched }

    @EventType
    private record DeliveryRecorded(Status status) {}

    @Test
    void anEnumPropertyOfAJavaEventWritesItsOrdinal() {
        assertEquals("{\"status\":1}", ChronicleGson.chronicleGson.toJson(new DeliveryRecorded(Status.Dispatched)));
    }

    @Test
    void aJavaEnumReadsNumbersNumericStringsAndCaseInsensitiveNames() {
        for (String json : new String[] {"1", "\"1\"", "\"dispatched\""}) {
            assertEquals(Status.Dispatched, ChronicleGson.chronicleGson.fromJson(json, Status.class));
        }
    }
}
