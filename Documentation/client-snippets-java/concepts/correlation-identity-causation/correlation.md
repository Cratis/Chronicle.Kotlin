```java
import io.cratis.chronicle.correlation.CorrelationIdManagerKt;
import java.util.UUID;

class CorrelationIdentityCausationCorrelation {
    UUID getCurrent() {
        return CorrelationIdManagerKt.getCorrelationIdManager().getCurrent();
    }

    void setForRequest() {
        CorrelationIdManagerKt.getCorrelationIdManager().set(UUID.randomUUID());
    }
}
```
