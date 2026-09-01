```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.java.EventLogJavaBridge;

@EventType
record SubjectAuthorRegistered(String name) {}

class SubjectAuthorService {
    private final IEventStore eventStore;

    SubjectAuthorService(IEventStore eventStore) {
        this.eventStore = eventStore;
    }

    void register(String authorId, String name) {
        // Subject defaults to authorId, so encryption keys for any PII on
        // SubjectAuthorRegistered are keyed by authorId.
        EventLogJavaBridge.append(
            eventStore.getEventLog(),
            authorId,
            new SubjectAuthorRegistered(name),
            null);
    }
}
```
