// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.spring

import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.observation.Reactor

/**
 * Sends a welcome package when someone is hired.
 *
 * Note the constructor: an artifact is activated through the Spring container, so it takes its
 * dependencies exactly like a `@Service` would. Returning an event appends it as a side effect on the
 * same event source, with no event log dependency in sight.
 */
@Reactor
class WelcomePackageReactor(private val mailer: Mailer) {
    fun employeeHired(event: EmployeeHired, context: EventContext): WelcomePackageRequested {
        mailer.send("${event.firstName}.${event.lastName}@cratis.io", "Welcome to the team!")
        return WelcomePackageRequested(employeeId = context.eventSourceId)
    }
}
