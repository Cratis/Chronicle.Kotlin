```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.RemovedWith
import io.cratis.chronicle.projections.RemovedWithJoin
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.readModels.ReadModel

@EventType(id = "mb-removal-multiple-account-opened")
data class MbRemovalMultipleAccountOpened(val name: String)

@EventType(id = "mb-removal-multiple-account-closed")
class MbRemovalMultipleAccountClosed

@EventType(id = "mb-removal-multiple-account-merged")
data class MbRemovalMultipleAccountMerged(val sourceAccountId: String)

@EventType(id = "mb-removal-multiple-organization-closed")
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
