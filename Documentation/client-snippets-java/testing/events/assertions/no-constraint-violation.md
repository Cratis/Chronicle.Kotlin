```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.eventSequences.AppendResult;
import io.cratis.chronicle.java.BlockingEventSequence;
import io.cratis.chronicle.testing.EventScenario;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * EventScenario runs in-process with no kernel behind it, so nothing enforces constraints and every
 * append comes back free of constraint violations - there is no kernel there to reject one.
 */
class TestingEventsAssertionsNoConstraintViolation {

    @EventType
    record AuthorRegistered(String name) {
    }

    @Test
    void anInProcessAppendNeverCarriesAConstraintViolation() {
        var scenario = new EventScenario("testing", "default");
        var eventLog = new BlockingEventSequence(scenario.getEventSequence());

        AppendResult result = eventLog.append("author-1", new AuthorRegistered("Jane Smith"));

        assertTrue(result.getConstraintViolations().isEmpty());
    }
}
```
