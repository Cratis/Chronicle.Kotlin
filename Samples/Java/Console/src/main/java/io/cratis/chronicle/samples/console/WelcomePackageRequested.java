// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console;

import io.cratis.chronicle.events.EventType;

/** A welcome package has been requested for a newly hired employee — a reactor side effect of {@link EmployeeHired}. */
@EventType
record WelcomePackageRequested(String employeeId) {}
