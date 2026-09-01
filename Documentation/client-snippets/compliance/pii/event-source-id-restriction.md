```kotlin
import io.cratis.chronicle.compliance.Pii
import io.cratis.chronicle.concepts.EventSourceId

// This will throw PiiNotSupportedOnEventSourceId
@Pii
data class PiiAttrEmployeeId(override val value: String) : EventSourceId
```
