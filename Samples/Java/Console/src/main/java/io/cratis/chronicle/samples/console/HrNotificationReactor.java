// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console;

import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.eventSequences.EventForEventSourceId;
import io.cratis.chronicle.observation.Reactor;

@Reactor
public class HrNotificationReactor {
    /** The shared event source the HR audit log lives on — every promotion is audited here, regardless of which employee it belongs to. */
    private static final String HR_AUDIT_LOG_EVENT_SOURCE_ID = "e0000001-0000-0000-0000-000000000000";

    /** Logs the hire and requests a welcome package for the new employee — a same-stream side effect via a bare return. */
    public WelcomePackageRequested employeeHired(EmployeeHired event, EventContext context) {
        System.out.println("[reactor] Employee hired: " + event.firstName() + " " +
                          event.lastName() + " as " + event.title() +
                          " (seq=" + context.getSequenceNumber() + ")");
        return new WelcomePackageRequested(context.getEventSourceId());
    }

    public void employeeAddressSet(EmployeeAddressSet event, EventContext context) {
        System.out.println("[reactor] Address set: " + event.city() + ", " +
                          event.country() + " (seq=" + context.getSequenceNumber() + ")");
    }

    public void employeeEmailSet(EmployeeEmailSet event, EventContext context) {
        System.out.println("[reactor] Email set: " + event.email() +
                          " (seq=" + context.getSequenceNumber() + ")");
    }

    /** Logs the promotion and records it in the shared HR audit log — a cross-stream side effect via {@link EventForEventSourceId}. */
    public EventForEventSourceId employeePromoted(EmployeePromoted event, EventContext context) {
        System.out.println("[reactor] Promoted to: " + event.newTitle() +
                          " (seq=" + context.getSequenceNumber() + ")");
        return new EventForEventSourceId(HR_AUDIT_LOG_EVENT_SOURCE_ID, new PromotionAudited(context.getEventSourceId(), event.newTitle()));
    }

    public void employeeMoved(EmployeeMoved event, EventContext context) {
        System.out.println("[reactor] Relocated to: " + event.city() + ", " +
                          event.country() + " (seq=" + context.getSequenceNumber() + ")");
    }
}
