// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console;

import io.cratis.chronicle.events.EventType;

/** An employee has been promoted to a new title. */
@EventType
public class EmployeePromoted {
    private String newTitle = "";

    public EmployeePromoted() {}

    public EmployeePromoted(String newTitle) {
        this.newTitle = newTitle;
    }

    public String getNewTitle() { return newTitle; }
    public void setNewTitle(String newTitle) { this.newTitle = newTitle; }
}
