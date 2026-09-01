```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.observation.Reactor
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
data class Book(val title: String = "")

@Reactor
class WaitlistNotifierWithBookTitle(private val notifications: NotificationService) {
    // Declaring the read model as a parameter resolves it for the event's event source -
    // strongly consistent, rebuilt from the event log, includes this event
    fun bookReturned(event: BookReturned, context: EventContext, book: Book?) {
        notifications.notifyNextInLine(context.eventSourceId, book?.title ?: "")
    }
}
```
