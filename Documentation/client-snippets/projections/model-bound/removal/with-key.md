```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.RemovedWith
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.readModels.ReadModel

@EventType(id = "mb-removal-with-key-account-opened")
data class MbRemovalWithKeyAccountOpened(val name: String)

@EventType(id = "mb-removal-with-key-account-closed")
data class MbRemovalWithKeyAccountClosed(val accountId: String)

@ReadModel
@FromEvent(MbRemovalWithKeyAccountOpened::class)
@RemovedWith(MbRemovalWithKeyAccountClosed::class, key = "accountId")
data class MbRemovalWithKeyAccount(
    @SetFrom("name", MbRemovalWithKeyAccountOpened::class)
    val name: String = ""
)
```
