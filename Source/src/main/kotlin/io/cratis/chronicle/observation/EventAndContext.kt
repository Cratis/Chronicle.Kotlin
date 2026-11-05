// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.cratis.chronicle.events.EventContext

/**
 * One event of an [EventBatch], together with its context.
 *
 * @property event The deserialized event - an instance of one of the types the batch method observes.
 * @property context The [EventContext] of the event: its event source, sequence number and the rest.
 */
data class EventAndContext(val event: Any, val context: EventContext)
