```kotlin
import io.cratis.chronicle.compliance.Pii
import io.cratis.chronicle.concepts.EventSourceId

// Throws PiiNotSupportedOnEventSourceId
@Pii
data class PiiConceptsEmployeeId(val value: String) : EventSourceId {
    override fun value(): String = value
}
```
