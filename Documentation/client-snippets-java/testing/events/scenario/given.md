```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.eventSequences.AppendResult;
import io.cratis.chronicle.java.BlockingEventSequence;
import io.cratis.chronicle.testing.EventScenario;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TestingEventsScenarioGiven {

    @EventType
    record AuthorRegistered(String name) {
    }

    @EventType
    record BookAdded(String title) {
    }

    @Test
    void preconditionsAppendedFirstAreAlreadyInTheLogBeforeTheAct() {
        var scenario = new EventScenario("testing", "default");
        var eventLog = new BlockingEventSequence(scenario.getEventSequence());
        var authorId = "author-1";

        // The precondition - what was already true before the code under test ran.
        eventLog.append(authorId, new AuthorRegistered("John Doe"));
        eventLog.append(authorId, new BookAdded("Clean Code"));

        // The act under test.
        AppendResult result = eventLog.append(authorId, new BookAdded("The Pragmatic Programmer"));

        assertTrue(result.isSuccess());
    }
}
```
