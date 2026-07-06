```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reactor

@EventType(id = "scenarios-react-book-returned")
data class ScenariosReactBookReturned(val isbn: String)

interface ScenariosReactNotificationService {
    fun notifyNextInLine(bookId: String)
    fun notifyNextInLine(bookId: String, bookTitle: String)
}

@Reactor
class ScenariosReactWaitlistNotifier(private val notifications: ScenariosReactNotificationService) {
    fun bookReturned(event: ScenariosReactBookReturned, context: EventContext) {
        // context.eventSourceId is the source the event happened to (the book)
        notifications.notifyNextInLine(context.eventSourceId)
    }
}
```
