```java
import io.cratis.chronicle.compliance.Pii;
import io.cratis.chronicle.events.EventType;

@EventType
record PiiAttrEmployeeRegistered(
        @Pii String firstName,
        @Pii String lastName,
        String department) {
}
```
