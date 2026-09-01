```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.eventSequences.AppendedEventWithResult;
import io.cratis.chronicle.eventSequences.AppendResult;
import io.cratis.chronicle.java.BlockingEventSequence;
import io.cratis.chronicle.testing.EventScenario;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestingEventsEventSequenceAssertionsAppendedEventWithResult {

    @EventType
    record AuthorRegistered(String name) {
    }

    @Test
    void pairingAnAppendedEventWithItsAppendResult() {
        var scenario = new EventScenario("testing", "default");
        var eventLog = new BlockingEventSequence(scenario.getEventSequence());
        var authorId = "author-1";
        var event = new AuthorRegistered("Jane Smith");

        AppendResult result = eventLog.append(authorId, event);
        var events = scenario.getEventLog().getEvents();
        var context = events.get(events.size() - 1).getContext();
        var collected = new AppendedEventWithResult(context, event, result);

        assertTrue(collected.getResult().isSuccess());
        assertEquals("Jane Smith", ((AuthorRegistered) collected.getEvent()).name());
        assertEquals(authorId, collected.getContext().getEventSourceId());
    }
}
```
