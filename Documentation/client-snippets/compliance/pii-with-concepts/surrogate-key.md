```kotlin
import io.cratis.chronicle.concepts.EventSourceId
import io.cratis.chronicle.events.EventType

// Surrogate key as event source identifier - not marked @Pii
data class PiiConceptsSurrogateEmployeeId(override val value: String) : EventSourceId

// Sensitive values stored in PII-marked concept types instead
@EventType(id = "pii-concepts-surrogate-employee-registered")
data class PiiConceptsSurrogateEmployeeRegistered(
    val nationalId: PiiConceptsNationalIdNumber,
    val name: PiiConceptsPersonName
)
```
