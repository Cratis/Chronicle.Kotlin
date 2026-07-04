```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.observation.Reactor

@Reactor
class GetStartedBookReturnedNotifier {
    fun returned(event: GetStartedBookReturned, context: EventContext) {
        // context.eventSourceId is the bookId this happened to
        println("Book ${context.eventSourceId} was returned — notify the next member in line.")
    }
}
```
