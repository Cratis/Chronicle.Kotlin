```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.eventSequences.AppendResult;
import io.cratis.chronicle.java.BlockingEventSequence;
import io.cratis.chronicle.testing.EventScenario;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TestingEventsScenarioBasic {

    @EventType
    record AuthorRegistered(String name) {
    }

    @Test
    void appendingAnEventThroughTheScenarioSucceeds() {
        var scenario = new EventScenario("testing", "default");
        var eventLog = new BlockingEventSequence(scenario.getEventSequence());

        AppendResult result = eventLog.append("author-1", new AuthorRegistered("John Doe"));

        assertTrue(result.isSuccess());
    }
}
```
