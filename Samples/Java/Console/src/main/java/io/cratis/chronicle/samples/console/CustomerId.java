// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console;

import io.cratis.chronicle.concepts.EventSourceId;

/**
 * Identifies a customer, and is the event source id every customer event is appended against.
 * <p>
 * Deliberately <b>not</b> marked {@code @Pii}, and the schema generator would refuse it if it were:
 * the event source id is what the kernel looks up the encryption key by, so encrypting the id would
 * make its own key unfindable. That is why this is a random surrogate id rather than something
 * personal like an email address — keep the personal values in the {@code @Pii} concepts alongside
 * this one.
 */
record CustomerId(String value) implements EventSourceId {
    @Override
    public String getValue() {
        return value;
    }
}
