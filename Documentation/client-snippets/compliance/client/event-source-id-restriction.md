```kotlin
import io.cratis.chronicle.compliance.Pii
import io.cratis.chronicle.concepts.EventSourceId

// This will throw PiiNotSupportedOnEventSourceId at registration
@Pii
data class ComplianceClientCustomerId(val value: String) : EventSourceId {
    override fun value(): String = value
}
```
