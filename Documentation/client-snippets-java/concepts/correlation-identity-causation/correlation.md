```java
import io.cratis.chronicle.correlation.CorrelationIdManagerKt;
import java.util.UUID;

// Per thread, not per request or coroutine; clear in finally on thread-pooled paths.
class CorrelationIdentityCausationCorrelation {
    UUID getCurrent() {
        return CorrelationIdManagerKt.getCorrelationIdManager().getCurrent();
    }

    void setForRequest() {
        CorrelationIdManagerKt.getCorrelationIdManager().set(UUID.randomUUID());
    }
}
```
