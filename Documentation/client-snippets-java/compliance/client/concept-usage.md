```java
import io.cratis.chronicle.events.EventType;

@EventType(id = "ComplianceClientEmployeeRegisteredWithConcept")
record ComplianceClientEmployeeRegisteredWithConcept(ComplianceClientPersonName name, String department) {
}

// also encrypted
@EventType(id = "ComplianceClientEmployeeNameChanged")
record ComplianceClientEmployeeNameChanged(ComplianceClientPersonName newName) {
}
```
