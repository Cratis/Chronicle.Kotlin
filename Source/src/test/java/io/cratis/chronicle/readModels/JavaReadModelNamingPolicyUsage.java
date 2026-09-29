// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.readModels;

import io.cratis.chronicle.ChronicleOptions;
import io.cratis.chronicle.connection.ChronicleConnectionString;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A Java caller supplying a read model naming policy as a lambda, the way an application would to match
 * the collection names another layer reads.
 */
class JavaReadModelNamingPolicyUsage {

    /** A read model without an explicit identifier. */
    public record Customer(String name) {
    }

    /** A read model with an explicit identifier. */
    @ReadModel(id = "invoice")
    public record InvoiceSummary(String number) {
    }

    @Test
    void a_lambda_becomes_the_policy_on_the_options() {
        var options = new ChronicleOptions(ChronicleConnectionString.Companion.getDEVELOPMENT())
            .withReadModelNamingPolicy(readModelClass -> readModelClass.getSimpleName() + "s");

        assertEquals("Customers", options.getReadModelNamingPolicy().getReadModelName(Customer.class));
    }

    @Test
    void the_default_policy_uses_the_read_model_identifier() {
        var options = new ChronicleOptions(ChronicleConnectionString.Companion.getDEVELOPMENT());

        assertEquals("Customer", options.getReadModelNamingPolicy().getReadModelName(Customer.class));
        assertEquals("invoice", options.getReadModelNamingPolicy().getReadModelName(InvoiceSummary.class));
    }
}
