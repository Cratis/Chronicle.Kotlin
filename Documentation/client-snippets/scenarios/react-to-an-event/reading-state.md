```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.observation.Reactor
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
data class ScenariosReactBook(val title: String = "")

@Reactor
class ScenariosReactWaitlistNotifierWithTitle(private val notifications: ScenariosReactNotificationService) {
    // Looked up by the triggering event's event source id; a materialized read model can lag this event or be null.
    fun bookReturned(event: ScenariosReactBookReturned, context: EventContext, book: ScenariosReactBook?) {
        notifications.notifyNextInLine(context.eventSourceId, book?.title ?: "")
    }
}
```
