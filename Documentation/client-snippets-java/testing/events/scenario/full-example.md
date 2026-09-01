```java
import com.google.gson.Gson;

import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.eventSequences.AppendResult;
import io.cratis.chronicle.java.BlockingEventSequence;
import io.cratis.chronicle.testing.EventScenario;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestingEventsScenarioFullExample {

    @EventType
    record AuthorRegistered(String name) {
    }

    @EventType
    record BookAdded(String title) {
    }

    @Test
    void theBookIsAppendedAfterTheAuthorIsRegistered() {
        var scenario = new EventScenario("testing", "default");
        var eventLog = new BlockingEventSequence(scenario.getEventSequence());
        var authorId = "author-1";
        var gson = new Gson();

        eventLog.append(authorId, new AuthorRegistered("Jane Smith"));

        AppendResult result = eventLog.append(authorId, new BookAdded("Clean Code"));

        assertTrue(result.isSuccess());

        var book = gson.fromJson(
            eventLog.getForEventSourceIdAndEventTypes(authorId, BookAdded.class).get(0).getContent(),
            BookAdded.class);
        assertEquals("Clean Code", book.title());
    }
}
```
