```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reactor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@EventType
data class VibeCancelled(val host: String)

data class CreateNotification(val host: String)

/** Returns the side effect as its result, so its logic is a pure function of the event. */
@Reactor
class CancellationReactor {
    fun vibeCancelled(event: VibeCancelled) = CreateNotification(event.host)
}

class CancellationReactorTests {

    @Test
    fun `a cancelled vibe requests a notification for its host`() {
        val reactor = CancellationReactor()

        val command = reactor.vibeCancelled(VibeCancelled("Ada"))

        assertEquals("Ada", command.host)
    }
}
```
