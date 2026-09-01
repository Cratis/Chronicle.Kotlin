```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.FilterEventsByTag
import io.cratis.chronicle.observation.Reactor

@EventType
data class FilterByTagMultiCustomerRegistered(val emailAddress: String = "")

@FilterEventsByTag("vip")
@FilterEventsByTag("priority")
@Reactor
class FilterByTagMultiPriorityNotificationsReactor {
    fun customerRegistered(event: FilterByTagMultiCustomerRegistered) {
        // Dispatched when the appended event carries either the "vip" or "priority" tag
    }
}
```
