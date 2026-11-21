```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.java.BlockingEventStore;
import io.cratis.chronicle.observation.Reactor;

@Reactor
class WaitlistNotifierWithExplicitAppend {
    private final NotificationService notifications;
    private final BlockingEventStore eventStore;

    WaitlistNotifierWithExplicitAppend(NotificationService notifications, IEventStore eventStore) {
        this.notifications = notifications;
        this.eventStore = new BlockingEventStore(eventStore);
    }

    void bookReturned(BookReturned event, EventContext context) {
        notifications.notifyNextInLine(context.getEventSourceId());
        eventStore.getEventLog().append(context.getEventSourceId(), new WaitlistNotificationSent());
    }
}
```
