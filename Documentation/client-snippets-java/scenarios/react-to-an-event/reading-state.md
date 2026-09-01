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

    // Declaring the read model as a parameter resolves it for the event's event source -
    // strongly consistent, rebuilt from the event log, includes this event
    void bookReturned(ScenariosReactBookReturned event, EventContext context, ScenariosReactBook book) {
        notifications.notifyNextInLine(context.getEventSourceId(), book != null ? book.title() : "");
    }
}
```
