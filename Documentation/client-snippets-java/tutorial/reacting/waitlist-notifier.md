```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.observation.Reactor;

interface NotificationService {
    void notifyNextInLine(String bookId);
    void notifyNextInLine(String bookId, String bookTitle);
}

@Reactor
class WaitlistNotifier {
    private final NotificationService notifications;

    WaitlistNotifier(NotificationService notifications) {
        this.notifications = notifications;
    }

    void bookReturned(BookReturned event, EventContext context) {
        // context.getEventSourceId() is the bookId this happened to
        notifications.notifyNextInLine(context.getEventSourceId());
    }
}
```
