```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reactor;

@EventType(id = "scenarios-react-book-returned")
record ScenariosReactBookReturned(String isbn) {}

interface ScenariosReactNotificationService {
    void notifyNextInLine(String bookId);

    void notifyNextInLine(String bookId, String bookTitle);
}

@Reactor
class ScenariosReactWaitlistNotifier {
    private final ScenariosReactNotificationService notifications;

    ScenariosReactWaitlistNotifier(ScenariosReactNotificationService notifications) {
        this.notifications = notifications;
    }

    void bookReturned(ScenariosReactBookReturned event, EventContext context) {
        // context.getEventSourceId() is the source the event happened to (the book)
        notifications.notifyNextInLine(context.getEventSourceId());
    }
}
```
