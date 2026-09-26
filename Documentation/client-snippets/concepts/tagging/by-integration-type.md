```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reactor
import io.cratis.chronicle.observation.Tag

@Tag("Notifications")
@Tag("ExternalAPI")
@Tag("MessageQueue")
@Tag("FileSystem")
@Reactor
class TaggingByIntegrationTypeExample {
    fun on(event: TaggingByIntegrationTypeRecordSynced) { }
}

@EventType
data class TaggingByIntegrationTypeRecordSynced(val id: String = "")
```
