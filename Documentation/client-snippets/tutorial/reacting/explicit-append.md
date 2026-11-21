```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.observation.Reactor

@Reactor
class WaitlistNotifierWithExplicitAppend(
    private val notifications: NotificationService,
    private val eventStore: IEventStore,
) {
    suspend fun bookReturned(event: BookReturned, context: EventContext) {
        notifications.notifyNextInLine(context.eventSourceId)
        eventStore.eventLog.append(context.eventSourceId, WaitlistNotificationSent())
    }
}
```
