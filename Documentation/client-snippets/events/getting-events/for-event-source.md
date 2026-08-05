```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.events.EventType

@EventType
data class EventsForSourceAccountOpened(val accountId: String = "", val ownerName: String = "")

suspend fun getAccountOpenedEvents(store: IEventStore, accountId: String) =
    store.eventLog.getForEventSourceIdAndEventTypes(accountId, listOf(EventsForSourceAccountOpened::class))
```
