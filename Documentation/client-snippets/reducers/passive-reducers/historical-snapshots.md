```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
data class PassiveReducersAccountBalance(val balance: Double = 0.0)

class PassiveReducersHistoricalBalanceService(private val eventStore: IEventStore) {
    // Passive reducer computes state on-demand from historical events
    suspend fun getBalanceAtDate(accountId: String): PassiveReducersAccountBalance? =
        eventStore.readModels.getInstanceByKey(PassiveReducersAccountBalance::class, accountId)
}
```
