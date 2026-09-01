```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

@EventType
data class DecJoinsMultipleEmployeeAssigned(val groupId: String, val departmentId: String, val locationId: String)

@EventType
data class DecJoinsMultipleGroupCreated(val name: String)

@EventType
data class DecJoinsMultipleDepartmentCreated(val name: String)

@EventType
data class DecJoinsMultipleLocationUpdated(val address: String)

data class DecJoinsMultipleEmployeeSummary(
    val groupId: String? = null,
    val groupName: String? = null,
    val departmentId: String? = null,
    val departmentName: String? = null,
    val locationId: String? = null,
    val locationAddress: String? = null
)

class DecJoinsMultipleEmployeeSummaryProjection : IProjectionFor<DecJoinsMultipleEmployeeSummary> {
    override fun define(builder: IProjectionBuilderFor<DecJoinsMultipleEmployeeSummary>) {
        builder
            .from(DecJoinsMultipleEmployeeAssigned::class)
            .join(DecJoinsMultipleGroupCreated::class) { it.on(DecJoinsMultipleEmployeeSummary::groupId) }
            .join(DecJoinsMultipleDepartmentCreated::class) { it.on(DecJoinsMultipleEmployeeSummary::departmentId) }
            .join(DecJoinsMultipleLocationUpdated::class) { it.on(DecJoinsMultipleEmployeeSummary::locationId) }
    }
}
```
