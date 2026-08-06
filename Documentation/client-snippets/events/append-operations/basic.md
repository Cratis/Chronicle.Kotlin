```kotlin
import io.cratis.chronicle.IEventStore
import kotlinx.coroutines.flow.collect

/**
 * Observes every append made through this specific [io.cratis.chronicle.eventSequences.IEventSequence]
 * instance — a hot flow, so only appends made after subscribing are seen.
 */
suspend fun watchAppends(store: IEventStore) {
    store.eventLog.appendOperations.collect { entries ->
        entries.forEach { entry ->
            println("${entry.context.eventType.id.value} appended at ${entry.result.sequenceNumber.value}")
        }
    }
}
```
