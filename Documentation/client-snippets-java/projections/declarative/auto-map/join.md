```java title="AutoMap with a join"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

@EventType
record AutoMapEmployeeHired(String employeeName, String departmentId) {}

@EventType
record AutoMapDepartmentRenamed(String departmentName) {}

class AutoMapEmployee {
    public String employeeName = "";
    public String departmentId = "";
    public String departmentName = "";
}

class AutoMapEmployeeProjection implements IProjectionFor<AutoMapEmployee> {
    @Override
    public void define(IProjectionBuilderFor<AutoMapEmployee> builder) {
        builder
            .from(AutoMapEmployeeHired.class)
            .join(AutoMapDepartmentRenamed.class, jb -> {
                jb.on("departmentId");
                return null; // Java lambda returning Unit
            });
    }
}
```
