```kotlin title="Multiple set mappings"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class AccountOpenedForRename(
    val accountName: String
)

@EventType
data class AccountRenamedForRename(
    val newName: String
)

@ReadModel
@FromEvent(AccountOpenedForRename::class)
@FromEvent(AccountRenamedForRename::class)
data class RenameableAccount(
    @SetFrom("accountName", AccountOpenedForRename::class)
    @SetFrom("newName", AccountRenamedForRename::class)
    val name: String = ""
)
```
