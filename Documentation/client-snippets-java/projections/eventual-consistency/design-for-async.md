```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.java.BlockingEventStore;
import java.util.UUID;

@EventType
record EcBookCreated(String title, String author) {}

record EcBookInventory(String id, String title, String author) {
    EcBookInventory() {
        this("", "", "");
    }
}

class EcBookService {
    private final BlockingEventStore store;

    EcBookService(IEventStore store) {
        this.store = new BlockingEventStore(store);
    }

    // Good — fire and forget: don't wait for the projection before returning
    String createBook(String title, String author) {
        var bookId = UUID.randomUUID().toString();

        store.getEventLog().append(bookId, new EcBookCreated(title, author));

        return bookId;
    }

    // Problematic — expecting immediate consistency
    EcBookInventory createBookAndReturn(String title, String author) {
        var bookId = createBook(title, author);

        // The projection may not have run yet — this can return null or a stale instance
        return store.getReadModels().getInstanceByKey(EcBookInventory.class, bookId);
    }
}
```
