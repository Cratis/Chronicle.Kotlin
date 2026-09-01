```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.eventSequences.AppendResult
import io.cratis.chronicle.eventSequences.EventForEventSourceId
import io.cratis.chronicle.events.EventType

@EventType(id = "TaggedMoneyWithdrawn")
data class TaggedMoneyWithdrawn(val amount: Double)

@EventType(id = "TaggedMoneyDeposited")
data class TaggedMoneyDeposited(val amount: Double)

class TaggedTransferService(private val eventStore: IEventStore) {
    suspend fun transfer(fromAccount: String, toAccount: String, amount: Double): List<AppendResult> {
        val events = listOf(
            EventForEventSourceId(fromAccount, TaggedMoneyWithdrawn(amount), tags = listOf("transfer", "audit")),
            EventForEventSourceId(toAccount, TaggedMoneyDeposited(amount), tags = listOf("transfer", "audit"))
        )

        return eventStore.eventLog.appendMany(events)
    }
}
```
