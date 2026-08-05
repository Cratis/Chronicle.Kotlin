// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.identities

import Cratis.Chronicle.Contracts.Identities.IdentitiesGrpcKt
import Cratis.Chronicle.Contracts.Identities.IdentitiesOuterClass
import com.google.protobuf.Empty
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class IdentityManagerServiceTests {

    @Test
    fun `rename sends the event store, namespace, subject and new name to the kernel`() = runBlocking {
        val stub = mockk<IdentitiesGrpcKt.IdentitiesCoroutineStub>()
        val request = slot<IdentitiesOuterClass.RenameIdentityRequest>()
        coEvery { stub.renameIdentity(capture(request), any()) } returns Empty.getDefaultInstance()

        val service = IdentityManagerService("my-event-store", "my-namespace", stub)
        service.rename("subject-123", "Jane Doe")

        assertEquals("my-event-store", request.captured.eventStore)
        assertEquals("my-namespace", request.captured.namespace)
        assertEquals("subject-123", request.captured.subject)
        assertEquals("Jane Doe", request.captured.name)
    }
}
