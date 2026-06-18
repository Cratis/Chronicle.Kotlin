// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console;

import io.cratis.chronicle.events.EventType;

/** An employee has relocated to a new address. */
@EventType
public record EmployeeMoved(
    String address,
    String city,
    String zipCode,
    String country
) {}
