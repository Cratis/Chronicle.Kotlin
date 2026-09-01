```kotlin
import io.cratis.chronicle.events.EventType

@EventType(id = "compliance-client-employee-registered-with-concept")
data class ComplianceClientEmployeeRegisteredWithConcept(val name: ComplianceClientPersonName, val department: String)

// also encrypted
@EventType(id = "compliance-client-employee-name-changed")
data class ComplianceClientEmployeeNameChanged(val newName: ComplianceClientPersonName)
```
