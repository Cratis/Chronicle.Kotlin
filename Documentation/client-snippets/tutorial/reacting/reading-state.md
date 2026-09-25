```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.observation.Reactor
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
data class Book(val title: String = "")

@Reactor
class WaitlistNotifierWithBookTitle(private val notifications: NotificationService) {
    // Looked up by the triggering event's event source id; a materialized read model can lag this event or be null.
    fun bookReturned(event: BookReturned, context: EventContext, book: Book?) {
        notifications.notifyNextInLine(context.eventSourceId, book?.title ?: "")
    }
}
```
