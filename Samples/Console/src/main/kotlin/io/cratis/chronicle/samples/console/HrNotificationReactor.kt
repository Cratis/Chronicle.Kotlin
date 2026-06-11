// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console

import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.observation.Reactor

@Reactor
class HrNotificationReactor {

    fun employeeHired(event: EmployeeHired, context: EventContext) {
        println("[reactor] Employee hired: ${event.firstName} ${event.lastName} as ${event.title} (seq=${context.sequenceNumber})")
    }

    fun employeeAddressSet(event: EmployeeAddressSet, context: EventContext) {
        println("[reactor] Address set: ${event.city}, ${event.country} (seq=${context.sequenceNumber})")
    }

    fun employeeEmailSet(event: EmployeeEmailSet, context: EventContext) {
        println("[reactor] Email set: ${event.email} (seq=${context.sequenceNumber})")
    }

    fun employeePromoted(event: EmployeePromoted, context: EventContext) {
        println("[reactor] Promoted to: ${event.newTitle} (seq=${context.sequenceNumber})")
    }

    fun employeeMoved(event: EmployeeMoved, context: EventContext) {
        println("[reactor] Relocated to: ${event.city}, ${event.country} (seq=${context.sequenceNumber})")
    }
}
