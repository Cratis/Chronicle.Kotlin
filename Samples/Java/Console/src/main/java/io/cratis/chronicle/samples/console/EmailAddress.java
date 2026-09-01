// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console;

import io.cratis.chronicle.compliance.Pii;
import io.cratis.chronicle.concepts.ConceptAs;

/** A customer's email address. */
@Pii(description = "Customer email address")
record EmailAddress(String value) implements ConceptAs<String> {
    @Override
    public String getValue() {
        return value;
    }
}
