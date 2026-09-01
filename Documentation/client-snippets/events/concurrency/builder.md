```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScopeBuilder

@EventType
data class ConcurrencyMoneyDeposited(val amount: Double = 0.0)

/**
 * Uses [ConcurrencyScopeBuilder] to fluently narrow a concurrency scope to this account's own
 * event source id and a specific event stream type.
 */
suspend fun processTransaction(store: IEventStore, accountId: String, amount: Double) {
    val concurrencyScope = ConcurrencyScopeBuilder()
        .withEventSourceId()
        .withSequenceNumber(EventSequenceNumber(15))
        .withEventStreamType("Transactions")
        .build()

    store.eventLog.append(
        accountId,
        ConcurrencyMoneyDeposited(amount),
        AppendOptions(eventStreamType = "Transactions", concurrencyScope = concurrencyScope)
    )
}
```
