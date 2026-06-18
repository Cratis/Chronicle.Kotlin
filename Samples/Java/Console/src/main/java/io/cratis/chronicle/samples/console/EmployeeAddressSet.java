// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console;

import io.cratis.chronicle.events.EventType;

/** An employee's address has been set. */
@EventType
public record EmployeeAddressSet(
    String address,
    String city,
    String zipCode,
    String country
) {}
