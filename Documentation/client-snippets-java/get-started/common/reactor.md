```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.observation.Reactor;

@Reactor
class GetStartedBookReturnedNotifier {
    void returned(GetStartedBookReturned event, EventContext context) {
        // context.getEventSourceId() is the bookId this happened to
        System.out.println("Book " + context.getEventSourceId() + " was returned — notify the next member in line.");
    }
}
```
