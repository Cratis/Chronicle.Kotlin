```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.observation.Reactor
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
data class ScenariosReactBook(val title: String = "")

@Reactor
class ScenariosReactWaitlistNotifierWithTitle(private val notifications: ScenariosReactNotificationService) {
    // Declaring the read model as a parameter resolves it for the event's event source -
    // strongly consistent, rebuilt from the event log, includes this event
    fun bookReturned(event: ScenariosReactBookReturned, context: EventContext, book: ScenariosReactBook?) {
        notifications.notifyNextInLine(context.eventSourceId, book?.title ?: "")
    }
}
```
