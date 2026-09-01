```kotlin title="AutoMap with a join"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

@EventType(id = "auto-map-employee-hired")
data class AutoMapEmployeeHired(val employeeName: String, val departmentId: String)

@EventType(id = "auto-map-department-renamed")
data class AutoMapDepartmentRenamed(val departmentName: String)

data class AutoMapEmployee(
    val employeeName: String = "",
    val departmentId: String = "",
    val departmentName: String = ""
)

class AutoMapEmployeeProjection : IProjectionFor<AutoMapEmployee> {
    override fun define(builder: IProjectionBuilderFor<AutoMapEmployee>) {
        builder
            .from(AutoMapEmployeeHired::class)
            .join(AutoMapDepartmentRenamed::class) {
                it.on(AutoMapEmployee::departmentId)
            }
    }
}
```
