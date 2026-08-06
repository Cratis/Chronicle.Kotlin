// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.identities

import Cratis.Chronicle.Contracts.Identities.IdentitiesGrpcKt
import Cratis.Chronicle.Contracts.Identities.IdentitiesOuterClass

class IdentityManagerService(
    private val eventStoreName: String,
    private val namespace: String,
    private val stub: IdentitiesGrpcKt.IdentitiesCoroutineStub
) : IIdentityManagerService {

    override suspend fun rename(subject: String, name: String) {
        val request = IdentitiesOuterClass.RenameIdentityRequest.newBuilder()
            .setEventStore(eventStoreName)
            .setNamespace(namespace)
            .setSubject(subject)
            .setName(name)
            .build()
        stub.renameIdentity(request)
    }
}
