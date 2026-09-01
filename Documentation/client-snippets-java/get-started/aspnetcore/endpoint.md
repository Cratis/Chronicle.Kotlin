```java
import io.cratis.chronicle.java.BlockingEventStore;

// Java has no ASP.NET Core minimal-API route builder to show here - the handler is a plain
// method taking the values a web framework would already have extracted from the request (a
// path variable and a query parameter), and appends the event directly through the blocking
// event store facade a Spring MVC handler can call without any coroutine bridging.
class AspNetCoreBookEndpoint {
    private final BlockingEventStore eventStore;

    AspNetCoreBookEndpoint(BlockingEventStore eventStore) {
        this.eventStore = eventStore;
    }

    void borrow(String bookId, String memberName) {
        eventStore.getEventLog().append(bookId, new GetStartedBookBorrowed(memberName));
    }
}
```
