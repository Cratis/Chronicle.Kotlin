```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.observation.Reactor

interface NotificationService {
    fun notifyNextInLine(bookId: String)
    fun notifyNextInLine(bookId: String, bookTitle: String)
}

@Reactor
class WaitlistNotifier(private val notifications: NotificationService) {
    fun bookReturned(event: BookReturned, context: EventContext) {
        // context.eventSourceId is the bookId this happened to
        notifications.notifyNextInLine(context.eventSourceId)
    }
}
```
