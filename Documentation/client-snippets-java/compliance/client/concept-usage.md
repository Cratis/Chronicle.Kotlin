```java
import io.cratis.chronicle.events.EventType;

@EventType
record ComplianceClientEmployeeRegisteredWithConcept(ComplianceClientPersonName name, String department) {
}

// also encrypted
@EventType
record ComplianceClientEmployeeNameChanged(ComplianceClientPersonName newName) {
}
```
