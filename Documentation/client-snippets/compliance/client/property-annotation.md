```kotlin
import io.cratis.chronicle.compliance.Pii
import io.cratis.chronicle.events.EventType
import java.time.Instant

@EventType(id = "compliance-client-employee-registered")
data class ComplianceClientEmployeeRegistered(
    @Pii val firstName: String,
    @Pii val lastName: String,
    val department: String,
    val startDate: Instant
)
```
