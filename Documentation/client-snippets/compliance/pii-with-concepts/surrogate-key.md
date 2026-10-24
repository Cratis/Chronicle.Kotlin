```kotlin
import io.cratis.chronicle.concepts.EventSourceId
import io.cratis.chronicle.events.EventType

// Surrogate key as event source identifier - not marked @Pii
data class PiiConceptsSurrogateEmployeeId(val value: String) : EventSourceId {
    override fun value(): String = value
}

// Sensitive values stored in PII-marked concept types instead
@EventType
data class PiiConceptsSurrogateEmployeeRegistered(
    val nationalId: PiiConceptsNationalIdNumber,
    val name: PiiConceptsPersonName
)
```
