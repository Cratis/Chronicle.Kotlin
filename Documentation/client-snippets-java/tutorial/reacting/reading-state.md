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

    // Declaring the read model as a parameter resolves it for the event's event source -
    // strongly consistent, rebuilt from the event log, includes this event
    void bookReturned(BookReturned event, EventContext context, Book book) {
        notifications.notifyNextInLine(context.getEventSourceId(), book != null ? book.title() : "");
    }
}
```
