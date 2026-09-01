// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console;

import io.cratis.chronicle.compliance.Pii;
import io.cratis.chronicle.concepts.ConceptAs;

/** The street part of a customer's address. */
@Pii(description = "Customer street address")
record StreetAddress(String value) implements ConceptAs<String> {
    @Override
    public String getValue() {
        return value;
    }
}
