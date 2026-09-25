```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reactor;
import io.cratis.chronicle.observation.Tag;

@Tag("Notifications")
@Tag("ExternalAPI")
@Tag("MessageQueue")
@Tag("FileSystem")
@Reactor
class TaggingByIntegrationTypeExample {
    void on(TaggingByIntegrationTypeRecordSynced event) { }
}

@EventType
record TaggingByIntegrationTypeRecordSynced(String id) {}
```
