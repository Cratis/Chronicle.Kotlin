```java
import io.cratis.chronicle.java.BlockingEventStore;

import java.util.UUID;

// A Spring CommandLineRunner bean (@Component class X implements CommandLineRunner) is the plain
// Spring Boot way to do work at startup in a host with no web layer. Reactors and projections are
// already running in the background once the starter has registered them - this method is what
// that runner's run() calls to append the first event.
class GetStartedWorker {
    private final BlockingEventStore eventStore;

    GetStartedWorker(BlockingEventStore eventStore) {
        this.eventStore = eventStore;
    }

    void run() {
        String bookId = UUID.randomUUID().toString();
        eventStore.getEventLog().append(bookId, new GetStartedBookAdded("The Pragmatic Programmer", "978-0135957059"));
    }
}
```
