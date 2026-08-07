```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.java.BlockingEventStore;

@EventType
record EcCqsBookCreated(String title) {}

record EcCqsBook(String id, String title) {
    EcCqsBook() {
        this("", "");
    }
}

// Commands — fire and forget, never return projected state
class EcCqsBookCommandHandler {
    private final BlockingEventStore store;

    EcCqsBookCommandHandler(IEventStore store) {
        this.store = new BlockingEventStore(store);
    }

    void create(String bookId, String title) {
        store.getEventLog().append(bookId, new EcCqsBookCreated(title));
    }
}

// Queries — always read from projections
class EcCqsBookQueryHandler {
    private final BlockingEventStore store;

    EcCqsBookQueryHandler(IEventStore store) {
        this.store = new BlockingEventStore(store);
    }

    EcCqsBook getBook(String bookId) {
        return store.getReadModels().getInstanceByKey(EcCqsBook.class, bookId);
    }
}
```
