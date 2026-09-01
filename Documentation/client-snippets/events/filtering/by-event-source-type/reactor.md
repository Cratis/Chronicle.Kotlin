```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.observation.EventSourceType
import io.cratis.chronicle.observation.Reactor

@EventType
data class FilterBySourceTypeCustomerRegistered(val emailAddress: String = "")

suspend fun registerByEventSourceType(store: IEventStore, eventSourceId: String, emailAddress: String) =
    store.eventLog.append(
        eventSourceId,
        FilterBySourceTypeCustomerRegistered(emailAddress),
        AppendOptions(eventSourceType = "customer")
    )

@EventSourceType("customer")
@Reactor
class FilterBySourceTypeCustomerWelcomeReactor {
    fun customerRegistered(event: FilterBySourceTypeCustomerRegistered, context: EventContext) {
        // Only invoked for events appended with eventSourceType: "customer"
    }
}
```
