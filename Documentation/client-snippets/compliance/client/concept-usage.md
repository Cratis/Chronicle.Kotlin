```kotlin
import io.cratis.chronicle.events.EventType

@EventType
data class ComplianceClientEmployeeRegisteredWithConcept(val name: ComplianceClientPersonName, val department: String)

// also encrypted
@EventType
data class ComplianceClientEmployeeNameChanged(val newName: ComplianceClientPersonName)
```
