```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.observation.Reactor

@Reactor
class WaitlistNotifierSideEffect(private val notifications: NotificationService) {
    // Returning an event appends it to the event log for you, against the triggering event's event source
    fun bookReturned(event: BookReturned, context: EventContext): WaitlistNotificationSent {
        notifications.notifyNextInLine(context.eventSourceId)
        return WaitlistNotificationSent()
    }
}
```
