// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.externalServices

import Cratis.Chronicle.Contracts.ExternalServices.ExternalServicesGrpcKt
import Cratis.Chronicle.Contracts.ExternalServices.Externalservices

class ExternalServicesService(
    private val eventStoreName: String,
    private val stub: ExternalServicesGrpcKt.ExternalServicesCoroutineStub
) : IExternalServicesService {

    override suspend fun register(name: String, configure: (IExternalServiceBuilder) -> Unit) {
        val builder = ExternalServiceBuilder()
        configure(builder)
        val definition = builder.build(name, name)

        val request = Externalservices.AddExternalServices.newBuilder()
            .setEventStore(eventStoreName)
            .addExternalServices(definition)
            .build()

        stub.add(request)
    }
}
