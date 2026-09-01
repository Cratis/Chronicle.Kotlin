```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
data class ReleasingCollectionSupportTicket(val id: String = "", val requesterName: String = "")

/**
 * Releases more than one instance at once - the subject for each is resolved independently, so
 * a single batch can freely mix data belonging to different people.
 */
suspend fun releaseAll(store: IEventStore, tickets: List<ReleasingCollectionSupportTicket>): List<ReleasingCollectionSupportTicket> =
    store.readModels.releaseMany(tickets)
```
