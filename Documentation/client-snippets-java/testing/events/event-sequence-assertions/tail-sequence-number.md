```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.java.BlockingEventSequence;
import io.cratis.chronicle.testing.EventScenario;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestingEventsEventSequenceAssertionsTailSequenceNumber {

    @EventType
    record AuthorRegistered(String name) {
    }

    @EventType
    record BookAdded(String title) {
    }

    @Test
    void theTailSequenceNumberIsThePositionOfTheLastEventAppended() {
        var scenario = new EventScenario("testing", "default");
        var eventLog = new BlockingEventSequence(scenario.getEventSequence());
        var authorId = "author-1";

        eventLog.append(authorId, new AuthorRegistered("Jane Smith"));
        eventLog.append(authorId, new BookAdded("Clean Code"));

        assertEquals(1L, eventLog.getTailSequenceNumber());
    }
}
```
