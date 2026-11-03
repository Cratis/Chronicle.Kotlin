```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.BatchContext
import io.cratis.chronicle.observation.EventBatch
import io.cratis.chronicle.observation.Observes
import io.cratis.chronicle.observation.Reducer

@EventType
data class BatchAccountOpened(val owner: String)

@EventType
data class BatchMoneyDeposited(val amount: Double)

@EventType
data class BatchMoneyWithdrawn(val amount: Double)

data class BatchAccount(val owner: String, val balance: Double)

@Reducer
class BatchAccountReducer {
    @Observes(BatchAccountOpened::class)
    @Observes(BatchMoneyDeposited::class)
    @Observes(BatchMoneyWithdrawn::class)
    fun reduce(events: EventBatch, current: BatchAccount?, context: BatchContext): BatchAccount? =
        events.fold(current) { account, e ->
            when (val event = e.event) {
                is BatchAccountOpened -> BatchAccount(event.owner, 0.0)
                is BatchMoneyDeposited -> account?.copy(balance = account.balance + event.amount)
                is BatchMoneyWithdrawn -> account?.copy(balance = account.balance - event.amount)
                else -> account
            }
        }
}
```
