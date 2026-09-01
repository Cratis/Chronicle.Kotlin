```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.java.BlockingEventSequence;
import io.cratis.chronicle.testing.EventScenario;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class TestingEventsEventSequenceAssertionsAppendedEventByType {

    @EventType
    record AuthorRegistered(String name) {
    }

    @EventType
    record BookAdded(String title) {
    }

    @Test
    void atLeastOneEventOfATypeWasAppendedSomewhereInTheSequence() {
        var scenario = new EventScenario("testing", "default");
        var eventLog = new BlockingEventSequence(scenario.getEventSequence());
        var authorId = "author-1";

        eventLog.append(authorId, new AuthorRegistered("Jane Smith"));
        eventLog.append(authorId, new BookAdded("Clean Code"));

        assertFalse(eventLog.getForEventSourceIdAndEventTypes(authorId, AuthorRegistered.class).isEmpty());
        assertFalse(eventLog.getForEventSourceIdAndEventTypes(authorId, BookAdded.class).isEmpty());
    }
}
```
