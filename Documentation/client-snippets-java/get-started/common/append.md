```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.java.BlockingEventStore;
import java.util.UUID;

class GetStartedBookService {
    private final BlockingEventStore eventStore;

    GetStartedBookService(IEventStore eventStore) {
        this.eventStore = new BlockingEventStore(eventStore);
    }

    String addBook() {
        var bookId = UUID.randomUUID().toString();

        eventStore.getEventLog().append(
            bookId,
            new GetStartedBookAdded("The Pragmatic Programmer", "978-0135957059"));

        return bookId;
    }
}
```
