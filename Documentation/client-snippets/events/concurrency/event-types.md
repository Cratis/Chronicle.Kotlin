```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.events.EventTypeDescriptor
import io.cratis.chronicle.events.EventTypeId
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScopeBuilder

@EventType
data class ConcurrencyPaymentProcessed(val amount: Double = 0.0)

@EventType
data class ConcurrencyPaymentFailed(val amount: Double = 0.0)

@EventType
data class ConcurrencyPaymentRefunded(val amount: Double = 0.0)

/**
 * Narrows the concurrency scope to only the payment-related event types, so other event types
 * appended for the same account don't affect this check.
 */
suspend fun processPayment(store: IEventStore, accountId: String, amount: Double) {
    val concurrencyScope = ConcurrencyScopeBuilder()
        .withEventSourceId()
        .withSequenceNumber(EventSequenceNumber(20))
        .withEventType(EventTypeDescriptor(EventTypeId("ConcurrencyPaymentProcessed")))
        .withEventType(EventTypeDescriptor(EventTypeId("ConcurrencyPaymentFailed")))
        .withEventType(EventTypeDescriptor(EventTypeId("ConcurrencyPaymentRefunded")))
        .build()

    store.eventLog.append(
        accountId,
        ConcurrencyPaymentProcessed(amount),
        AppendOptions(concurrencyScope = concurrencyScope)
    )
}
```
