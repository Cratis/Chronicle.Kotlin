```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

@EventType
record DecJoinsMultipleEmployeeAssigned(String groupId, String departmentId, String locationId) {}

@EventType
record DecJoinsMultipleGroupCreated(String name) {}

@EventType
record DecJoinsMultipleDepartmentCreated(String name) {}

@EventType
record DecJoinsMultipleLocationUpdated(String address) {}

class DecJoinsMultipleEmployeeSummary {
    public String groupId = null;
    public String groupName = null;
    public String departmentId = null;
    public String departmentName = null;
    public String locationId = null;
    public String locationAddress = null;
}

class DecJoinsMultipleEmployeeSummaryProjection implements IProjectionFor<DecJoinsMultipleEmployeeSummary> {
    @Override
    public void define(IProjectionBuilderFor<DecJoinsMultipleEmployeeSummary> builder) {
        builder
            .from(DecJoinsMultipleEmployeeAssigned.class)
            .join(DecJoinsMultipleGroupCreated.class, jb -> {
                jb.on("groupId");
                return null; // Java lambda returning Unit
            })
            .join(DecJoinsMultipleDepartmentCreated.class, jb -> {
                jb.on("departmentId");
                return null; // Java lambda returning Unit
            })
            .join(DecJoinsMultipleLocationUpdated.class, jb -> {
                jb.on("locationId");
                return null; // Java lambda returning Unit
            });
    }
}
```
