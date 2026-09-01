```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
data class ReleasingSingleInstanceSupportTicket(val id: String = "", val requesterName: String = "")

suspend fun release(store: IEventStore, ticket: ReleasingSingleInstanceSupportTicket): ReleasingSingleInstanceSupportTicket =
    store.readModels.release(ticket)
```
