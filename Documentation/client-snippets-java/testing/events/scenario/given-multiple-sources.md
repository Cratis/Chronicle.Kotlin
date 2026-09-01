```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.java.BlockingEventSequence;
import io.cratis.chronicle.testing.EventScenario;

import org.junit.jupiter.api.Test;

class TestingEventsScenarioGivenMultipleSources {

    @EventType
    record AuthorRegistered(String name) {
    }

    @Test
    void preconditionsSeedDifferentEventSourcesIndependently() {
        var scenario = new EventScenario("testing", "default");
        var eventLog = new BlockingEventSequence(scenario.getEventSequence());

        eventLog.append("author-1", new AuthorRegistered("Jane Smith"));
        eventLog.append("author-2", new AuthorRegistered("John Doe"));
    }
}
```
