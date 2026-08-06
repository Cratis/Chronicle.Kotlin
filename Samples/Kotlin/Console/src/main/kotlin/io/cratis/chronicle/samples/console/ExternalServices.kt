// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console

import io.cratis.chronicle.EventStore

/**
 * Registers the external payroll provider Chronicle can call out to — e.g. from a webhook or a
 * reactor-triggered HTTP call — as an [io.cratis.chronicle.externalServices.IExternalServicesService]
 * HTTP endpoint secured with a bearer token.
 */
suspend fun registerPayrollExternalService(store: EventStore) {
    store.externalServices.register("payroll-provider") { builder ->
        builder
            .http("https://payroll.example.com/api")
            .withBearerToken("demo-payroll-integration-token")
    }
    println("[external-services] Registered 'payroll-provider' as an HTTP external service.")
}
