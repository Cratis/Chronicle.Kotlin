```kotlin
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.eventSequences.IEventSequence
import kotlin.reflect.KClass

/**
 * Computes the tail sequence number relevant to a specific observer, based on the event types
 * it handles.
 */
suspend fun getRelevantTail(eventSequence: IEventSequence, observerType: KClass<*>): EventSequenceNumber =
    eventSequence.getTailSequenceNumberForObserver(observerType)
```
