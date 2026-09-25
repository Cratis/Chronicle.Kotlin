// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

import Cratis.Chronicle.Contracts.Clients.Clients
import Cratis.Chronicle.Contracts.Clients.ConnectionServiceGrpcKt
import com.google.protobuf.ByteString
import io.grpc.Channel
import java.util.concurrent.TimeUnit

/** Verifies a newly dialed channel before any application stub can use it. */
internal object CompatibilityPreflight {
    suspend fun verify(channel: Channel) {
        val descriptor = ConnectionServiceGrpcKt::class.java.getResourceAsStream("/chronicle/chronicle.desc")
            ?.use { it.readBytes() }
            ?.takeIf { it.isNotEmpty() }
            ?: error("The Chronicle contracts descriptor is missing; no operations were sent.")
        val request = Clients.CompatibilityRequest.newBuilder()
            .setClientType("Kotlin")
            .setClientVersion(ChronicleConnection::class.java.`package`.implementationVersion ?: "unknown")
            .setProtocolVersion(ConnectionServiceGrpcKt::class.java.`package`.implementationVersion ?: "unknown")
            .setDescriptorSet(ByteString.copyFrom(descriptor))
            .build()
        val response = ConnectionServiceGrpcKt.ConnectionServiceCoroutineStub(channel)
            .withDeadlineAfter(10, TimeUnit.SECONDS)
            .checkCompatibility(request)
        if (!response.isCompatible || response.incompatibilitiesCount != 0) {
            throw ChronicleServerIncompatible(
                "Chronicle server ${response.serverVersion} is incompatible: ${response.incompatibilitiesList.joinToString("; ")}. No operations were sent."
            )
        }
    }
}
