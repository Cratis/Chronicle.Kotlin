```java
import io.cratis.chronicle.concepts.EventSourceId;
import io.cratis.chronicle.events.EventType;

// Surrogate key as event source identifier - not marked @Pii
record PiiConceptsSurrogateEmployeeId(String value) implements EventSourceId {
    @Override
    public String getValue() {
        return value;
    }
}

// Sensitive values stored in PII-marked concept types instead
@EventType(id = "PiiConceptsSurrogateEmployeeRegistered")
record PiiConceptsSurrogateEmployeeRegistered(
        PiiConceptsNationalIdNumber nationalId,
        PiiConceptsPersonName name) {
}
```
