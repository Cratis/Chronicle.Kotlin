```java
import io.cratis.chronicle.compliance.Pii;
import io.cratis.chronicle.concepts.EventSourceId;

// This will throw PiiNotSupportedOnEventSourceId at registration
@Pii
record ComplianceClientCustomerId(String value) implements EventSourceId {
}
```
