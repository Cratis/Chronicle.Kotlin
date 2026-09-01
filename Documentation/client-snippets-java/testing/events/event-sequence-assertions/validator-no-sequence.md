```java
import com.google.gson.Gson;

import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.java.BlockingEventSequence;
import io.cratis.chronicle.testing.EventScenario;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestingEventsEventSequenceAssertionsValidatorNoSequence {

    @EventType
    record AuthorRegistered(String name) {
    }

    @Test
    void theFirstMatchingEventAnywhereInTheSequenceCarriesTheExpectedContent() {
        var scenario = new EventScenario("testing", "default");
        var eventLog = new BlockingEventSequence(scenario.getEventSequence());
        var gson = new Gson();

        eventLog.append("author-1", new AuthorRegistered("Jane Smith"));

        var author = gson.fromJson(scenario.getEventLog().getEvents().get(0).getContent(), AuthorRegistered.class);

        assertEquals("Jane Smith", author.name());
    }
}
```
