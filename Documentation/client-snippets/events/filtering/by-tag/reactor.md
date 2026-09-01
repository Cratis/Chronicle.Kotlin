```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.observation.FilterEventsByTag
import io.cratis.chronicle.observation.Reactor

@EventType
data class FilterByTagCustomerRegistered(val emailAddress: String = "")

suspend fun registerByTag(store: IEventStore, eventSourceId: String, emailAddress: String) =
    store.eventLog.append(
        eventSourceId,
        FilterByTagCustomerRegistered(emailAddress),
        AppendOptions(tags = listOf("vip", "onboarding"))
    )

@FilterEventsByTag("vip")
@Reactor
class FilterByTagVipWelcomeReactor {
    fun customerRegistered(event: FilterByTagCustomerRegistered, context: EventContext) {
        // Only receives events appended with the "vip" tag
    }
}
```
