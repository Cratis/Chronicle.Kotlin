```kotlin
import io.cratis.chronicle.compliance.Pii
import io.cratis.chronicle.events.EventType

@EventType(id = "pii-attr-employee-registered")
data class PiiAttrEmployeeRegistered(
    @Pii val firstName: String,
    @Pii val lastName: String,
    val department: String
)
```
