```kotlin
import io.cratis.chronicle.correlation.correlationIdManager
import java.util.UUID

// Per thread, not per request or coroutine; clear in finally on thread-pooled paths.
class CorrelationIdentityCausationCorrelation {
    fun getCurrent(): UUID = correlationIdManager.current

    fun setForRequest() = correlationIdManager.set(UUID.randomUUID())
}
```
