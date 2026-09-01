```java
import com.google.gson.Gson;

import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.java.BlockingEventSequence;
import io.cratis.chronicle.testing.EventScenario;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestingEventsEventSequenceAssertionsFullExample {

    @EventType
    record AuthorRegistered(String name) {
    }

    @EventType
    record BookAdded(String title) {
    }

    @Test
    void bothEventsLandInOrder() {
        var scenario = new EventScenario("testing", "default");
        var eventLog = new BlockingEventSequence(scenario.getEventSequence());
        var authorId = "author-1";
        var gson = new Gson();

        eventLog.append(authorId, new AuthorRegistered("Jane Smith"));
        eventLog.append(authorId, new BookAdded("Clean Code"));

        assertEquals(1L, eventLog.getTailSequenceNumber());

        var events = scenario.getEventLog().getEvents();
        var author = gson.fromJson(events.get(0).getContent(), AuthorRegistered.class);
        var book = gson.fromJson(events.get(1).getContent(), BookAdded.class);

        assertEquals("Jane Smith", author.name());
        assertEquals("Clean Code", book.title());
    }
}
```
