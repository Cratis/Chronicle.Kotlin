// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.spring;

import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.observation.Reactor;

/**
 * Sends a welcome package when someone is hired.
 *
 * <p>Note the constructor: an artifact is activated through the Spring container, so it takes its
 * dependencies exactly like a {@code @Service} would. Returning an event appends it as a side effect on
 * the same event source, with no event log dependency in sight.
 */
@Reactor
public class WelcomePackageReactor {
    private final Mailer mailer;

    public WelcomePackageReactor(Mailer mailer) {
        this.mailer = mailer;
    }

    public WelcomePackageRequested employeeHired(EmployeeHired event, EventContext context) {
        mailer.send(event.firstName() + "." + event.lastName() + "@cratis.io", "Welcome to the team!");
        return new WelcomePackageRequested(context.getEventSourceId());
    }
}
