// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console;

import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.observation.Reactor;

@Reactor
public class HrNotificationReactor {

    public void employeeHired(EmployeeHired event, EventContext context) {
        System.out.println("[reactor] Employee hired: " + event.firstName() + " " + 
                          event.lastName() + " as " + event.title() + 
                          " (seq=" + context.getSequenceNumber() + ")");
    }

    public void employeeAddressSet(EmployeeAddressSet event, EventContext context) {
        System.out.println("[reactor] Address set: " + event.city() + ", " + 
                          event.country() + " (seq=" + context.getSequenceNumber() + ")");
    }

    public void employeeEmailSet(EmployeeEmailSet event, EventContext context) {
        System.out.println("[reactor] Email set: " + event.email() + 
                          " (seq=" + context.getSequenceNumber() + ")");
    }

    public void employeePromoted(EmployeePromoted event, EventContext context) {
        System.out.println("[reactor] Promoted to: " + event.newTitle() + 
                          " (seq=" + context.getSequenceNumber() + ")");
    }

    public void employeeMoved(EmployeeMoved event, EventContext context) {
        System.out.println("[reactor] Relocated to: " + event.city() + ", " + 
                          event.country() + " (seq=" + context.getSequenceNumber() + ")");
    }
}
