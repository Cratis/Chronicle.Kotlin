// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console;

import io.cratis.chronicle.events.EventType;

/** An employee has been hired into the organization. */
@EventType
public record EmployeeHired(
    String firstName,
    String lastName,
    String title
) {}
