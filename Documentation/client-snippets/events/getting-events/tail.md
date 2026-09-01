```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.eventSequences.AppendedEvent
import io.cratis.chronicle.eventSequences.EventSequenceNumber

/**
 * Reads from a computed start position and trims in memory to the requested count.
 */
suspend fun readLast(store: IEventStore, count: Int): List<AppendedEvent> {
    val tail = store.eventLog.getTailSequenceNumber()
    val start = if (tail.isActualValue && tail.value >= count - 1) {
        EventSequenceNumber(tail.value - (count - 1))
    } else {
        EventSequenceNumber.first
    }

    val events = store.eventLog.getFromSequenceNumber(start)
    return events.takeLast(count)
}
```
