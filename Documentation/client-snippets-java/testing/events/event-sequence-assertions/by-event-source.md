```java
import com.google.gson.Gson;

import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.java.BlockingEventSequence;
import io.cratis.chronicle.testing.EventScenario;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestingEventsEventSequenceAssertionsByEventSource {

    @EventType
    record AuthorRegistered(String name) {
    }

    @Test
    void anAppendedEventIsScopedToItsOwnEventSource() {
        var scenario = new EventScenario("testing", "default");
        var eventLog = new BlockingEventSequence(scenario.getEventSequence());
        var author1 = "author-1";
        var author2 = "author-2";
        var gson = new Gson();

        eventLog.append(author1, new AuthorRegistered("Jane Smith"));
        eventLog.append(author2, new AuthorRegistered("John Doe"));

        var first = gson.fromJson(
            eventLog.getForEventSourceIdAndEventTypes(author1, AuthorRegistered.class).get(0).getContent(),
            AuthorRegistered.class);
        var second = gson.fromJson(
            eventLog.getForEventSourceIdAndEventTypes(author2, AuthorRegistered.class).get(0).getContent(),
            AuthorRegistered.class);

        assertEquals("Jane Smith", first.name());
        assertEquals("John Doe", second.name());
    }
}
```
