```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.events.EventTypeDescriptor
import io.cratis.chronicle.events.EventTypeId
import io.cratis.chronicle.eventSequences.AppendResult
import io.cratis.chronicle.eventSequences.EventForEventSourceId
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScopeBuilder

@EventType
data class ConcurrencyMoneyWithdrawnForTransfer(val amount: Double = 0.0)

@EventType
data class ConcurrencyMoneyDepositedForTransfer(val amount: Double = 0.0)

/**
 * Appends to two event sources as one atomic batch, each checked against its own expected
 * sequence number and narrowed to the event type it produces.
 */
suspend fun transferMoney(store: IEventStore, fromAccount: String, toAccount: String, amount: Double): List<AppendResult> {
    val events = listOf(
        EventForEventSourceId(fromAccount, ConcurrencyMoneyWithdrawnForTransfer(amount)),
        EventForEventSourceId(toAccount, ConcurrencyMoneyDepositedForTransfer(amount))
    )

    val concurrencyScopes = mapOf(
        fromAccount to ConcurrencyScopeBuilder()
            .withEventSourceId()
            .withSequenceNumber(EventSequenceNumber(50))
            .withEventType(EventTypeDescriptor(EventTypeId("ConcurrencyMoneyWithdrawnForTransfer")))
            .build(),
        toAccount to ConcurrencyScopeBuilder()
            .withEventSourceId()
            .withSequenceNumber(EventSequenceNumber(25))
            .withEventType(EventTypeDescriptor(EventTypeId("ConcurrencyMoneyDepositedForTransfer")))
            .build()
    )

    return store.eventLog.appendMany(events, concurrencyScopes)
}
```
