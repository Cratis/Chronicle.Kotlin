```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.eventSequences.EventSequenceNumber

suspend fun getEventsSince(store: IEventStore, sequenceNumber: Long, accountId: String) =
    store.eventLog.getFromSequenceNumber(EventSequenceNumber(sequenceNumber), eventSourceId = accountId)
```
