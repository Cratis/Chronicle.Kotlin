```java
import com.google.gson.Gson;

import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.java.BlockingEventSequence;
import io.cratis.chronicle.testing.EventScenario;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TestingEventsEventSequenceAssertionsPredicate {

    @EventType
    record AuthorRegistered(String name) {
    }

    @Test
    void anAppendedEventSatisfiesACondition() {
        var scenario = new EventScenario("testing", "default");
        var eventLog = new BlockingEventSequence(scenario.getEventSequence());
        var gson = new Gson();

        eventLog.append("author-1", new AuthorRegistered("Jane Smith"));

        var matches = scenario.getEventLog().getEvents().stream()
            .map(appended -> gson.fromJson(appended.getContent(), AuthorRegistered.class))
            .anyMatch(author -> author.name().equals("Jane Smith"));

        assertTrue(matches);
    }
}
```
