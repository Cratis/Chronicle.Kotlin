// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.compliance

import Cratis.Chronicle.Contracts.Compliance.ComplianceGrpcKt
import Cratis.Chronicle.Contracts.Compliance.ComplianceOuterClass

class ComplianceService(
    private val eventStoreName: String,
    private val namespace: String,
    private val stub: ComplianceGrpcKt.ComplianceCoroutineStub
) : IComplianceService {

    override suspend fun release(subject: String, schema: String, payload: String): String {
        val request = ComplianceOuterClass.ReleaseRequest.newBuilder()
            .setEventStore(eventStoreName)
            .setNamespace(namespace)
            .setSubject(subject)
            .setSchema(schema)
            .setPayload(payload)
            .build()

        val response = stub.release(request)
        return response.payload
    }
}
