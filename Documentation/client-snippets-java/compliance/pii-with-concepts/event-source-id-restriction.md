```java
import io.cratis.chronicle.compliance.Pii;
import io.cratis.chronicle.concepts.EventSourceId;

// Throws PiiNotSupportedOnEventSourceId
@Pii
record PiiConceptsEmployeeId(String value) implements EventSourceId {
    @Override
    public String getValue() {
        return value;
    }
}
```
