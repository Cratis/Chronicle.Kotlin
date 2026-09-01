```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.events.EventType

@EventType
data class EventsIndexLogEmployeeRegistered(val firstName: String = "", val lastName: String = "")

/**
 * The event log is the default event sequence, exposed through [IEventStore.eventLog].
 */
suspend fun registerEmployee(store: IEventStore, employeeId: String, firstName: String, lastName: String) =
    store.eventLog.append(employeeId, EventsIndexLogEmployeeRegistered(firstName, lastName))
```
