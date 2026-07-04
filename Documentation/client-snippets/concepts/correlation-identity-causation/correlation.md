```kotlin
import io.cratis.chronicle.correlation.correlationIdManager
import java.util.UUID

class CorrelationIdentityCausationCorrelation {
    fun getCurrent(): UUID = correlationIdManager.current

    fun setForRequest() = correlationIdManager.set(UUID.randomUUID())
}
```
