// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.webhooks

import Cratis.Chronicle.Contracts.Observation.Webhooks.ObservationWebhooks
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WebhookDefinitionBuilderTests {
    @Test
    fun `a default webhook is replayable and active`() {
        val definition = WebhookDefinitionBuilder().build("id", "http://localhost/hook")

        assertFlags(definition, isReplayable = true, isActive = true)
    }

    @Test
    fun `a non-replayable webhook remains active`() {
        val builder = WebhookDefinitionBuilder()
        builder.notReplayable()

        assertFlags(builder.build("id", "http://localhost/hook"), isReplayable = false, isActive = true)
    }

    @Test
    fun `an inactive webhook remains replayable`() {
        val builder = WebhookDefinitionBuilder()
        builder.notActive()

        assertFlags(builder.build("id", "http://localhost/hook"), isReplayable = true, isActive = false)
    }

    @Test
    fun `a webhook can be both non-replayable and inactive`() {
        val builder = WebhookDefinitionBuilder()
        builder.notReplayable().notActive()

        assertFlags(builder.build("id", "http://localhost/hook"), isReplayable = false, isActive = false)
    }

    private fun assertFlags(
        definition: ObservationWebhooks.WebhookDefinition,
        isReplayable: Boolean,
        isActive: Boolean,
    ) {
        assertEquals(isReplayable, definition.isReplayable)
        assertEquals(!isReplayable, definition.isNotReplayable)
        assertEquals(isActive, definition.isActive)
        assertEquals(!isActive, definition.isInactive)
    }
}
