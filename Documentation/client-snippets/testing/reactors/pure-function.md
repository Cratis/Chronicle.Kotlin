```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reactor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@EventType
data class VibeCancelled(val host: String)

@EventType
data class NotificationRequested(val host: String)

/** Returns an event that Chronicle appends; its logic is a pure function of the input event. */
@Reactor
class CancellationReactor {
    fun vibeCancelled(event: VibeCancelled) = NotificationRequested(event.host)
}

class CancellationReactorTests {

    @Test
    fun `a cancelled vibe requests a notification for its host`() {
        val reactor = CancellationReactor()

        val notification = reactor.vibeCancelled(VibeCancelled("Ada"))

        assertEquals("Ada", notification.host)
    }
}
```
