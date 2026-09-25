```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reactor
import io.cratis.chronicle.observation.Tag

@Tag("Customer")
@Tag("Operations")
@Tag("Finance")
@Tag("Support")
@Tag("Executive")
@Reactor
class TaggingByStakeholderExample {
    fun on(event: TaggingByStakeholderReportRequested) { }
}

@EventType
data class TaggingByStakeholderReportRequested(val id: String = "")
```
