```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.Join
import io.cratis.chronicle.projections.RemovedWithJoin
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.readModels.ReadModel

@EventType(id = "mb-removal-join-class-employee-hired")
data class MbRemovalJoinClassEmployeeHired(val name: String)

@EventType(id = "mb-removal-join-class-company-registered")
data class MbRemovalJoinClassCompanyRegistered(val name: String)

@EventType(id = "mb-removal-join-class-company-dissolved")
class MbRemovalJoinClassCompanyDissolved

@ReadModel
@FromEvent(MbRemovalJoinClassEmployeeHired::class)
@RemovedWithJoin(MbRemovalJoinClassCompanyDissolved::class)
data class MbRemovalJoinClassEmployee(
    @SetFrom("name", MbRemovalJoinClassEmployeeHired::class)
    val name: String = "",

    @Join(MbRemovalJoinClassCompanyRegistered::class, eventPropertyName = "name")
    val companyName: String = ""
)
```
