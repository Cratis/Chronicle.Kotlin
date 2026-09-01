// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console;

import io.cratis.chronicle.concepts.ConceptAs;

/**
 * The country a customer resides in.
 * <p>
 * Not marked {@code @Pii} — a country on its own identifies nobody. It is a concept anyway, so it
 * cannot be passed where a {@link City} or a {@link PostalCode} was expected.
 */
record Country(String value) implements ConceptAs<String> {
    @Override
    public String getValue() {
        return value;
    }
}
