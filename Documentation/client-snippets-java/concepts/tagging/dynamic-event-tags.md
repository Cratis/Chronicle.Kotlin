```java
import io.cratis.chronicle.eventSequences.IEventLog;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.java.AppendOptionsBuilder;
import io.cratis.chronicle.java.EventLogJavaBridge;

@EventType
record TaggingUserLoggedIn(String userId) {}

class TaggingUserLoginService {
    private final IEventLog eventLog;

    TaggingUserLoginService(IEventLog eventLog) {
        this.eventLog = eventLog;
    }

    void recordLogin(String eventSourceId) {
        EventLogJavaBridge.append(
            eventLog,
            eventSourceId,
            new TaggingUserLoggedIn("user123"),
            new AppendOptionsBuilder().tag("production").tag("critical").build());
    }
}
```
