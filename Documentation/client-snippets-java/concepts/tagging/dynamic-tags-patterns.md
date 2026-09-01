```java
import io.cratis.chronicle.eventSequences.IEventLog;
import io.cratis.chronicle.events.EventType;

import io.cratis.chronicle.java.AppendOptionsBuilder;
import io.cratis.chronicle.java.EventLogJavaBridge;

@EventType
record TaggingDynamicTagsEventOccurred(String data) {}

class TaggingDynamicTagsService {
    private final IEventLog eventLog;

    TaggingDynamicTagsService(IEventLog eventLog) {
        this.eventLog = eventLog;
    }

    void recordProductionCritical(String eventSourceId) {
        EventLogJavaBridge.append(
            eventLog,
            eventSourceId,
            new TaggingDynamicTagsEventOccurred("production issue"),
            new AppendOptionsBuilder().tag("production").tag("critical").build());
    }

    void recordDevelopmentTest(String eventSourceId) {
        EventLogJavaBridge.append(
            eventLog,
            eventSourceId,
            new TaggingDynamicTagsEventOccurred("test run"),
            new AppendOptionsBuilder().tag("development").tag("testing").build());
    }

    void recordBatchMigration(String eventSourceId) {
        EventLogJavaBridge.append(
            eventLog,
            eventSourceId,
            new TaggingDynamicTagsEventOccurred("batch migration"),
            new AppendOptionsBuilder().tag("migration").tag("batch-process").build());
    }
}
```
