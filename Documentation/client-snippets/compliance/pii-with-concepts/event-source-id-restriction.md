```kotlin
import io.cratis.chronicle.compliance.Pii
import io.cratis.chronicle.concepts.EventSourceId

// Throws PiiNotSupportedOnEventSourceId
@Pii
data class PiiConceptsEmployeeId(override val value: String) : EventSourceId
```
