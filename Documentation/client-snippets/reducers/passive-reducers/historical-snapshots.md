```kotlin
// Requires io.cratis:chronicle 6.5.0 or later.
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class PassiveReducersMoneyDeposited(val amount: Double = 0.0)

@ReadModel
data class PassiveReducersAccountBalance(val balance: Double = 0.0)

// Passive: nothing is stored; each read folds the account's events in the client.
@Reducer(isActive = false)
class PassiveReducersAccountBalanceReducer {
    fun deposited(event: PassiveReducersMoneyDeposited, state: PassiveReducersAccountBalance?) =
        PassiveReducersAccountBalance((state?.balance ?: 0.0) + event.amount)
}

class PassiveReducersHistoricalBalanceService(private val eventStore: IEventStore) {
    // Returns the balance as of now. The JVM client has no read-as-of-a-date call; for past states,
    // read the snapshots with getSnapshotsById and pick the one that occurred before the date.
    suspend fun getCurrentBalance(accountId: String): PassiveReducersAccountBalance? =
        eventStore.readModels.getInstanceByKey(PassiveReducersAccountBalance::class, accountId)
}
```
