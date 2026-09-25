```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.observation.Reactor;
import io.cratis.chronicle.readModels.ReadModel;

@ReadModel
record Book(String title) {
    Book() {
        this("");
    }
}

@Reactor
class WaitlistNotifierWithBookTitle {
    private final NotificationService notifications;

    WaitlistNotifierWithBookTitle(NotificationService notifications) {
        this.notifications = notifications;
    }

    // Looked up by the triggering event's event source id; a materialized read model can lag this event or be null.
    void bookReturned(BookReturned event, EventContext context, Book book) {
        notifications.notifyNextInLine(context.getEventSourceId(), book != null ? book.title() : "");
    }
}
```
