// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console;

import io.cratis.chronicle.events.EventType;

/** An employee has been promoted to a new title. */
@EventType
public record EmployeePromoted(String newTitle) {}
