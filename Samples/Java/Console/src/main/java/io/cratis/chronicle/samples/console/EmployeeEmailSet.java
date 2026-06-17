// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console;

import io.cratis.chronicle.events.EventType;

/** An employee's email address has been set. */
@EventType
public class EmployeeEmailSet {
    private String email = "";

    public EmployeeEmailSet() {}

    public EmployeeEmailSet(String email) {
        this.email = email;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
