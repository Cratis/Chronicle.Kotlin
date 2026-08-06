// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.samples.console;

import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.Join;
import io.cratis.chronicle.projections.NotRewindable;
import io.cratis.chronicle.readModels.ReadModel;

/**
 * A read model over payroll runs ingested from the external payroll system.
 *
 * {@link PayrollRunCompleted} only carries the employee's id, not their name, so {@link #employeeFirstName}
 * and {@link #employeeLastName} are pulled in with {@link Join} against this store's own {@link EmployeeHired}
 * event, correlated on the read model's own {@code id} (which equals the employee id here).
 *
 * Marked {@link NotRewindable} because a rewind would require the payroll system to redeliver events
 * through its outbox, which it may no longer have available.
 */
@ReadModel
@FromEvent(eventType = PayrollRunCompleted.class)
@NotRewindable
public class PayrollRunSummary {
    private String id = "";
    private double amount = 0;
    @Join(eventType = EmployeeHired.class, on = "id", eventPropertyName = "firstName")
    private String employeeFirstName = "";
    @Join(eventType = EmployeeHired.class, on = "id", eventPropertyName = "lastName")
    private String employeeLastName = "";

    public PayrollRunSummary() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getEmployeeFirstName() { return employeeFirstName; }
    public void setEmployeeFirstName(String employeeFirstName) { this.employeeFirstName = employeeFirstName; }

    public String getEmployeeLastName() { return employeeLastName; }
    public void setEmployeeLastName(String employeeLastName) { this.employeeLastName = employeeLastName; }
}
