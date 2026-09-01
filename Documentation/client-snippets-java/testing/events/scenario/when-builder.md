```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.eventSequences.AppendResult;
import io.cratis.chronicle.java.BlockingEventSequence;
import io.cratis.chronicle.testing.EventScenario;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * There is no separate builder for the act phase - the act under test is simply the direct call to
 * append, and its return value is the "then".
 */
class TestingEventsScenarioWhenBuilder {

    @EventType
    record AuthorRegistered(String name) {
    }

    @Test
    void theActAppendsTheEventUnderTestAndReturnsItsResult() {
        var scenario = new EventScenario("testing", "default");
        var eventLog = new BlockingEventSequence(scenario.getEventSequence());
        var existingAuthorId = "author-1";
        var newAuthorId = "author-2";

        // Given: an author is already registered.
        eventLog.append(existingAuthorId, new AuthorRegistered("John Doe"));

        // When: register a different author under a new event source - the act returns the result.
        AppendResult result = eventLog.append(newAuthorId, new AuthorRegistered("John Doe"));

        // Then: assert on the returned result.
        assertTrue(result.isSuccess());
    }
}
```
