// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console

import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.eventSequences.EventForEventSourceId
import io.cratis.chronicle.observation.Reactor

/** The shared event source the HR audit log lives on — every promotion is audited here, regardless of which employee it belongs to. */
private const val hrAuditLogEventSourceId = "e0000001-0000-0000-0000-000000000000"

@Reactor
class HrNotificationReactor {

    /** Logs the hire and requests a welcome package for the new employee — a same-stream side effect via a bare return. */
    fun employeeHired(event: EmployeeHired, context: EventContext): WelcomePackageRequested {
        println("[reactor] Employee hired: ${event.firstName} ${event.lastName} as ${event.title} (seq=${context.sequenceNumber})")
        return WelcomePackageRequested(employeeId = context.eventSourceId)
    }

    fun employeeAddressSet(event: EmployeeAddressSet, context: EventContext) {
        println("[reactor] Address set: ${event.city}, ${event.country} (seq=${context.sequenceNumber})")
    }

    fun employeeEmailSet(event: EmployeeEmailSet, context: EventContext) {
        println("[reactor] Email set: ${event.email} (seq=${context.sequenceNumber})")
    }

    /** Logs the promotion and records it in the shared HR audit log — a cross-stream side effect via [EventForEventSourceId]. */
    fun employeePromoted(event: EmployeePromoted, context: EventContext): EventForEventSourceId {
        println("[reactor] Promoted to: ${event.newTitle} (seq=${context.sequenceNumber})")
        return EventForEventSourceId(hrAuditLogEventSourceId, PromotionAudited(context.eventSourceId, event.newTitle))
    }

    fun employeeMoved(event: EmployeeMoved, context: EventContext) {
        println("[reactor] Relocated to: ${event.city}, ${event.country} (seq=${context.sequenceNumber})")
    }
}
