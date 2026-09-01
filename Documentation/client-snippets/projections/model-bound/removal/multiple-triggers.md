```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.RemovedWith
import io.cratis.chronicle.projections.RemovedWithJoin
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class MbRemovalMultipleAccountOpened(val name: String)

@EventType
class MbRemovalMultipleAccountClosed

@EventType
data class MbRemovalMultipleAccountMerged(val sourceAccountId: String)

@EventType
class MbRemovalMultipleOrganizationClosed

@ReadModel
@FromEvent(MbRemovalMultipleAccountOpened::class)
@RemovedWith(MbRemovalMultipleAccountClosed::class)
@RemovedWith(MbRemovalMultipleAccountMerged::class, key = "sourceAccountId")
@RemovedWithJoin(MbRemovalMultipleOrganizationClosed::class)
data class MbRemovalMultipleAccount(
    @SetFrom("name", MbRemovalMultipleAccountOpened::class)
    val name: String = ""
)
```
