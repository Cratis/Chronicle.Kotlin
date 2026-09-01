```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.java.ProjectionsServiceJavaBridge;
import io.cratis.chronicle.projections.ProjectionQueryResult;

class InboxMessagesQuery {
    ProjectionQueryResult getInboxMessages(EventStore store) {
        return ProjectionsServiceJavaBridge.query(
            store.getProjections(),
            "projection InboxMessages\n  from MessageReceived",
            "inbox"
        );
    }
}
```
