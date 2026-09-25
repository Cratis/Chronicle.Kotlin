```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
data class PassiveReducersAccountBalance(val balance: Double = 0.0)

class PassiveReducersHistoricalBalanceService(private val eventStore: IEventStore) {
    // Fold the event log on read to get the current balance, not a balance at a chosen date.
    suspend fun getCurrentBalance(accountId: String): PassiveReducersAccountBalance? =
        eventStore.readModels.getInstanceByKey(PassiveReducersAccountBalance::class, accountId)
}
```
