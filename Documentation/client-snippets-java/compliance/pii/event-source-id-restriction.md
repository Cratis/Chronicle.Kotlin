```java
import io.cratis.chronicle.compliance.Pii;
import io.cratis.chronicle.concepts.EventSourceId;

// This will throw PiiNotSupportedOnEventSourceId
@Pii
record PiiAttrEmployeeId(String value) implements EventSourceId {
    @Override
    public String getValue() {
        return value;
    }
}
```
