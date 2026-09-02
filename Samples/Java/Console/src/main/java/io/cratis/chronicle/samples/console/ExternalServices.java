// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console;

import io.cratis.chronicle.EventStore;

import io.cratis.chronicle.java.ExternalServicesServiceJavaBridge;

/**
 * Registers the external payroll provider Chronicle can call out to — e.g. from a webhook or a
 * reactor-triggered HTTP call — as an HTTP endpoint secured with a bearer token.
 */
public class ExternalServices {
    public static void registerPayrollExternalService(EventStore store) {
        ExternalServicesServiceJavaBridge.register(store.getExternalServices(), "payroll-provider", builder -> {
            builder
                .http("https://payroll.example.com/api")
                .withBearerToken("demo-payroll-integration-token");
        });
        System.out.println("[external-services] Registered 'payroll-provider' as an HTTP external service.");
    }
}
