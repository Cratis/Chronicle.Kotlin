```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reactor
import io.cratis.chronicle.observation.Tag

@Tag("Analytics")
@Tag("Reporting")
@Tag("Integration")
@Tag("Alerting")
@Tag("Monitoring")
@Tag("Automation")
@Reactor
class TaggingByPurposeExample {
    fun on(event: TaggingByPurposeCustomerRegistered) { }
}

@EventType
data class TaggingByPurposeCustomerRegistered(val id: String = "")
```
