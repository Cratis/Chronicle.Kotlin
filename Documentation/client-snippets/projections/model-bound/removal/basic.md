```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.RemovedWith
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.readModels.ReadModel

@EventType(id = "mb-removal-account-opened")
data class MbRemovalAccountOpened(val name: String, val balance: Double)

@EventType(id = "mb-removal-account-closed")
class MbRemovalAccountClosed

@ReadModel
@FromEvent(MbRemovalAccountOpened::class)
@RemovedWith(MbRemovalAccountClosed::class)
data class MbRemovalAccount(
    @SetFrom("name", MbRemovalAccountOpened::class)
    val name: String = "",

    @SetFrom("balance", MbRemovalAccountOpened::class)
    val balance: Double = 0.0
)
```
