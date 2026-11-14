// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs;

import io.cratis.chronicle.keys.Key;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Graph views written in Java, the way a reader would write them: records for the nodes, edges and views,
 * annotations on the components, and the definitions read through the public API.
 *
 * A Kotlin-only spec cannot prove that this compiles for Java, or that the annotations Java puts on a record
 * component are found.
 */
public class JavaGraphViewUsage {

    /** A node as a record. */
    @Graph("JavaStaff")
    @Node
    public record Employee(@Key String employeeId, String name) {
    }

    /** A node as a record. */
    @Graph("JavaStaff")
    @Node
    public record Department(@Key String departmentId, String title) {
    }

    /** An edge with a property. */
    @Graph("JavaStaff")
    @Edge("WORKS_IN")
    public record WorksIn(
        @EdgeSource(Employee.class) String employeeId,
        @EdgeTarget(Department.class) String departmentId,
        String since) {
    }

    /** An edge without properties. */
    @Graph("JavaStaff")
    @Edge("MANAGES")
    public record Manages(
        @EdgeSource(Employee.class) String employeeId,
        @EdgeTarget(Department.class) String departmentId) {
    }

    /** A view that reads a property of the edge it was reached by. */
    @ViewOf(Department.class)
    public record DepartmentMembership(@Key String id, String title, @FromEdge String since) {
    }

    /** A view that reads a collection through an edge. */
    @ViewOf(Employee.class)
    public record EmployeeWithDepartments(
        @Key String id,
        String name,
        @Follows(WorksIn.class) List<DepartmentMembership> departments) {
    }

    /** A view that only reads properties. */
    @ViewOf(Employee.class)
    public record EmployeeSummary(@Key String id, String name) {
    }

    /** A view that reads a single value through an edge, followed backwards. */
    @ViewOf(value = Department.class, name = "ManagedDepartment")
    public record DepartmentWithManager(
        @Key String id,
        @Follows(value = Manages.class, direction = FollowDirection.Incoming) EmployeeSummary manager) {
    }

    /** A view anchored at something that is not a node. */
    @ViewOf(String.class)
    public record Orphan(String name) {
    }

    @Test
    public void viewsDeclaredWithRecordsGiveADefinition() {
        var definitions = GraphViews.definitionsFor(
            Employee.class, Department.class, WorksIn.class, Manages.class,
            DepartmentMembership.class, EmployeeWithDepartments.class, EmployeeSummary.class, DepartmentWithManager.class);

        assertEquals(1, definitions.size());
        var definition = definitions.get(0);
        assertEquals("JavaStaff", definition.getGraph());
        assertEquals(
            List.of("DepartmentMembership", "EmployeeSummary", "EmployeeWithDepartments", "ManagedDepartment"),
            definition.getViews().stream().map(GraphView::getName).toList());

        var json = definition.toJson();
        assertTrue(json.contains(
            "{ \"name\": \"departments\", \"kind\": \"relation\", \"edge\": \"WORKS_IN\", "
                + "\"direction\": \"outgoing\", \"cardinality\": \"many\", \"view\": \"DepartmentMembership\" }"));
        assertTrue(json.contains(
            "{ \"name\": \"manager\", \"kind\": \"relation\", \"edge\": \"MANAGES\", "
                + "\"direction\": \"incoming\", \"cardinality\": \"one\", \"view\": \"EmployeeSummary\" }"));
        assertTrue(json.contains("{ \"name\": \"id\", \"kind\": \"property\", \"source\": \"departmentId\" }"));
        assertTrue(json.contains("{ \"name\": \"since\", \"kind\": \"edgeProperty\", \"source\": \"since\" }"));
        assertTrue(json.endsWith("}\n"));
        assertTrue(definition.toJson(true).contains("\"schema\""));
    }

    @Test
    public void aViewThatBreaksARuleExposesItsReason() {
        var failure = assertThrows(
            InvalidGraphView.class,
            () -> GraphViews.definitionsFor(Employee.class, Department.class, WorksIn.class, Orphan.class));

        assertTrue(failure instanceof ViewRootNotANode);
        assertEquals("ViewRootNotANode", failure.getReason());
        assertEquals(ViewRootNotANode.REASON, failure.getReason());
    }
}
