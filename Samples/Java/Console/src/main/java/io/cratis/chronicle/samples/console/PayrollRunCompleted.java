// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console;

import io.cratis.chronicle.events.EventType;

/**
 * A payroll run reported by the external payroll system. Owned by the payroll event store — this
 * store only ever sees it through the payroll-inbox subscription's outbox, but it still needs to be
 * registered locally so this store knows the event's shape.
 */
@EventType
record PayrollRunCompleted(String employeeId, double amount) {}
