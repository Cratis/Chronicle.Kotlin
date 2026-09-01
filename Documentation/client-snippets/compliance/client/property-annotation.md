```kotlin
import io.cratis.chronicle.compliance.Pii
import io.cratis.chronicle.events.EventType
import java.time.Instant

@EventType
data class ComplianceClientEmployeeRegistered(
    @Pii val firstName: String,
    @Pii val lastName: String,
    val department: String,
    val startDate: Instant
)
```
