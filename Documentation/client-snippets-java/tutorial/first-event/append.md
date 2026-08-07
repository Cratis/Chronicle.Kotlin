```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.java.BlockingEventStore;
import java.util.UUID;

class TutorialFirstEventAppend {
    String addBook(IEventStore eventStore) {
        var bookId = UUID.randomUUID().toString();

        new BlockingEventStore(eventStore).getEventLog().append(
            bookId,
            new BookAdded("The Pragmatic Programmer", "978-0135957059"));

        return bookId;
    }
}
```
