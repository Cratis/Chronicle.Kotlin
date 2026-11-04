// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

import io.cratis.chronicle.auditing.Causation
import java.time.Instant
import java.util.UUID

/**
 * Optional values for [IEventSequence.compensate].
 *
 * The event source, event stream and subject are deliberately absent: a compensation always goes to
 * those of the event it compensates for.
 *
 * @property correlationId The correlation identifier of the event. Defaults to the current one.
 * @property tags Tags to associate with the event, combined with any the event type declares.
 * @property occurred When the event occurred. The kernel uses approximately the time of the append when omitted.
 * @property causation The causation chain to record. Defaults to the current chain.
 */
data class CompensateOptions @JvmOverloads constructor(
    val correlationId: UUID? = null,
    val tags: List<String> = emptyList(),
    val occurred: Instant? = null,
    val causation: List<Causation> = emptyList()
)
