// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.webhooks

import Cratis.Chronicle.Contracts.Observation.Webhooks.ObservationWebhooks
import Cratis.Chronicle.Contracts.Observation.Webhooks.WebhooksGrpcKt
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WebhooksServiceTests {
    @Test
    fun `an inactive non-replayable response reconstructs both companion flags`() = runBlocking {
        val response = ObservationWebhooks.WebhookDetailsResponse.newBuilder()
            .setIsReplayable(false)
            .setIsActive(false)
            .build()

        val definition = definitionFrom(response)

        assertFalse(definition.isReplayable)
        assertTrue(definition.isNotReplayable)
        assertFalse(definition.isActive)
        assertTrue(definition.isInactive)
    }

    @Test
    fun `an active replayable response leaves both companion flags false`() = runBlocking {
        val response = ObservationWebhooks.WebhookDetailsResponse.newBuilder()
            .setIsReplayable(true)
            .setIsActive(true)
            .build()

        val definition = definitionFrom(response)

        assertTrue(definition.isReplayable)
        assertFalse(definition.isNotReplayable)
        assertTrue(definition.isActive)
        assertFalse(definition.isInactive)
    }

    @Test
    fun `an empty response reconstructs both companion flags as true`() = runBlocking {
        val definition = definitionFrom(ObservationWebhooks.WebhookDetailsResponse.getDefaultInstance())

        assertFalse(definition.isReplayable)
        assertTrue(definition.isNotReplayable)
        assertFalse(definition.isActive)
        assertTrue(definition.isInactive)
    }

    private suspend fun definitionFrom(
        response: ObservationWebhooks.WebhookDetailsResponse,
    ): ObservationWebhooks.WebhookDefinition {
        val stub = mockk<WebhooksGrpcKt.WebhooksCoroutineStub>()
        coEvery { stub.getWebhooks(any(), any()) } returns
            ObservationWebhooks.QueryResult_IEnumerable_WebhookDetailsResponse.newBuilder()
                .addData(response)
                .build()

        return WebhooksService("store", stub).getAll().single()
    }
}
