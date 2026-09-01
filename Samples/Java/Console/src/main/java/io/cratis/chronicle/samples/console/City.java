// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console;

import io.cratis.chronicle.compliance.Pii;
import io.cratis.chronicle.concepts.ConceptAs;

/** The city a customer resides in. */
@Pii(description = "City of residence")
record City(String value) implements ConceptAs<String> {
    @Override
    public String getValue() {
        return value;
    }
}
