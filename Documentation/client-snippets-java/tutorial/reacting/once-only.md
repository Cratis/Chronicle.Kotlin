```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.observation.OnceOnly;
import io.cratis.chronicle.observation.Reactor;

interface OverdueLetterService {
    void sendOverdueLetter(String bookId);
}

@Reactor
class OverdueLetterSender {
    private final OverdueLetterService letters;

    OverdueLetterSender(OverdueLetterService letters) {
        this.letters = letters;
    }

    // A posted letter cannot be un-posted, so this handler is excluded from every replay.
    @OnceOnly
    void bookReturned(BookReturned event, EventContext context) {
        letters.sendOverdueLetter(context.getEventSourceId());
    }
}
```
