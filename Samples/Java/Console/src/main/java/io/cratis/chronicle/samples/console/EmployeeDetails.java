// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console;

import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.readModels.ReadModel;

/**
 * Model-bound projection artifact discovered by the Kotlin client.
 *
 * Each {@link FromEvent} annotation on the class declares which event types contribute to this read model.
 * Properties are auto-mapped by name unless overridden with {@link SetFrom}.
 */
@ReadModel
@FromEvent(eventType = EmployeeHired.class)
@FromEvent(eventType = EmployeeAddressSet.class)
@FromEvent(eventType = EmployeePromoted.class)
@FromEvent(eventType = EmployeeMoved.class)
public class EmployeeDetails {
    private String id = "";
    private String firstName = "";
    private String lastName = "";
    @SetFrom(propertyPath = "newTitle", eventType = EmployeePromoted.class)
    private String title = "";
    private String address = "";
    private String city = "";
    private String zipCode = "";
    private String country = "";

    public EmployeeDetails() {}

    public EmployeeDetails(String id, String firstName, String lastName, String title,
                          String address, String city, String zipCode, String country) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.title = title;
        this.address = address;
        this.city = city;
        this.zipCode = zipCode;
        this.country = country;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getZipCode() { return zipCode; }
    public void setZipCode(String zipCode) { this.zipCode = zipCode; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
}
