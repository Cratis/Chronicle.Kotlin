```kotlin
import io.cratis.chronicle.compliance.Pii
import io.cratis.chronicle.concepts.EventSourceId

// This will throw PiiNotSupportedOnEventSourceId
@Pii
data class PiiAttrEmployeeId(val value: String) : EventSourceId {
    override fun value(): String = value
}
```
