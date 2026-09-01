```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.observation.Reactor;

@Reactor
class WaitlistNotifierSideEffect {
    private final NotificationService notifications;

    WaitlistNotifierSideEffect(NotificationService notifications) {
        this.notifications = notifications;
    }

    // Returning an event appends it to the event log for you, against the triggering event's event source
    WaitlistNotificationSent bookReturned(BookReturned event, EventContext context) {
        notifications.notifyNextInLine(context.getEventSourceId());
        return new WaitlistNotificationSent();
    }
}
```
