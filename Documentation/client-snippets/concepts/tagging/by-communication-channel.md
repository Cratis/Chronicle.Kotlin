```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reactor
import io.cratis.chronicle.observation.Tag

@Tag("Email")
@Tag("SMS")
@Tag("Push")
@Tag("Webhook")
@Reactor
class TaggingByCommunicationChannelExample {
    fun on(event: TaggingByCommunicationChannelMessageSent) { }
}

@EventType
data class TaggingByCommunicationChannelMessageSent(val id: String = "")
```
