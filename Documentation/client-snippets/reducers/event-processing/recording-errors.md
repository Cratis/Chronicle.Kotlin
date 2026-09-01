```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class EventProcessingInvalidDataDetected(val reason: String)

@ReadModel
data class EventProcessingValidationResult(val isValid: Boolean = true, val errors: List<String> = emptyList())

@Reducer
class EventProcessingValidationResultReducer {
    fun detected(event: EventProcessingInvalidDataDetected, current: EventProcessingValidationResult?): EventProcessingValidationResult {
        val errors = (current?.errors ?: emptyList()) + event.reason

        return EventProcessingValidationResult(false, errors)
    }
}
```
