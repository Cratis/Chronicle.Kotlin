```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.readModels.IReadModelReactor
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class AccountFlagged(val accountId: String = "")

@ReadModel
data class ReactingSideEffectsAccount(val id: String = "", val balance: Double = 0.0)

class AccountReviewer : IReadModelReactor {
    /**
     * Returning an event from a handler appends it, using the changed instance's key as the
     * event source id by default.
     */
    fun modified(account: ReactingSideEffectsAccount): AccountFlagged = AccountFlagged(account.id)
}
```
