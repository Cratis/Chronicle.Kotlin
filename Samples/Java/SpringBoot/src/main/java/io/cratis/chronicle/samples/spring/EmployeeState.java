// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.spring;

import io.cratis.chronicle.readModels.ReadModel;

/** What an employee looks like right now, folded together from everything that has happened to them. */
@ReadModel
public class EmployeeState {
    private String id = "";
    private String firstName = "";
    private String lastName = "";
    private String title = "";
    private String email = "";

    public EmployeeState() {
    }

    public EmployeeState(String id, String firstName, String lastName, String title, String email) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.title = title;
        this.email = email;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
