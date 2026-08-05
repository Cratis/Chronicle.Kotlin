```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.observation.OnceOnly
import io.cratis.chronicle.observation.Reactor

interface OverdueLetterService {
    fun sendOverdueLetter(bookId: String)
}

@Reactor
class OverdueLetterSender(private val letters: OverdueLetterService) {
    // A posted letter cannot be un-posted, so this handler is excluded from every replay.
    @OnceOnly
    fun bookReturned(event: BookReturned, context: EventContext) {
        letters.sendOverdueLetter(context.eventSourceId)
    }
}
```
