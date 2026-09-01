```java
import io.cratis.chronicle.compliance.Pii;
import io.cratis.chronicle.events.EventType;

import java.time.Instant;

@EventType
record ComplianceClientEmployeeRegistered(
        @Pii String firstName,
        @Pii String lastName,
        String department,
        Instant startDate) {
}
```
