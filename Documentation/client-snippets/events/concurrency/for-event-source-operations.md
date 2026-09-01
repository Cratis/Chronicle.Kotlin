```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.events.EventTypeDescriptor
import io.cratis.chronicle.events.EventTypeId
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.eventSequences.operations.forEventSourceId

@EventType
class ConcurrencyAccountValidated

@EventType
class ConcurrencyAccountProcessed

/**
 * Composes two events against the same event source, with a shared concurrency scope narrowed
 * to the event types this operation produces.
 */
suspend fun processAccountBatch(store: IEventStore, accountId: String) {
    store.eventLog
        .forEventSourceId(accountId) {
            withConcurrencyScope {
                withSequenceNumber(EventSequenceNumber(30))
                withEventType(EventTypeDescriptor(EventTypeId("ConcurrencyAccountProcessed")))
                withEventType(EventTypeDescriptor(EventTypeId("ConcurrencyAccountValidated")))
            }
            append(ConcurrencyAccountValidated())
            append(ConcurrencyAccountProcessed())
        }
        .perform()
}
```
