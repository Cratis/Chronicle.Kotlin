```kotlin
import io.cratis.chronicle.eventSequences.AppendedEventWithResult
import kotlinx.coroutines.flow.SharedFlow

/**
 * The shape of [io.cratis.chronicle.eventSequences.IEventSequence.appendOperations] - a hot
 * [SharedFlow] that emits after every [io.cratis.chronicle.eventSequences.IEventSequence.append]
 * or [io.cratis.chronicle.eventSequences.IEventSequence.appendMany] call made through that instance.
 */
interface EventSequenceAppendOperationsShape {
    val appendOperations: SharedFlow<List<AppendedEventWithResult>>
}
```
