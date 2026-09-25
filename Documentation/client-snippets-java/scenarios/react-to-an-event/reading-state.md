```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.observation.Reactor;
import io.cratis.chronicle.readModels.ReadModel;

@ReadModel
record ScenariosReactBook(String title) {
    ScenariosReactBook() {
        this("");
    }
}

@Reactor
class ScenariosReactWaitlistNotifierWithTitle {
    private final ScenariosReactNotificationService notifications;

    ScenariosReactWaitlistNotifierWithTitle(ScenariosReactNotificationService notifications) {
        this.notifications = notifications;
    }

    // Looked up by the triggering event's event source id; a materialized read model can lag this event or be null.
    void bookReturned(ScenariosReactBookReturned event, EventContext context, ScenariosReactBook book) {
        notifications.notifyNextInLine(context.getEventSourceId(), book != null ? book.title() : "");
    }
}
```
