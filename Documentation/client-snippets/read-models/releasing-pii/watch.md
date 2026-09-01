```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.readModels.ReadModel
import kotlinx.coroutines.flow.collect

@ReadModel
data class ReleasingWatchSupportTicket(val id: String = "", val requesterName: String = "")

/**
 * [io.cratis.chronicle.readModels.IReadModelsService.watch] is the one built-in read that does
 * not release PII automatically - release each change yourself as it arrives.
 */
suspend fun watchTickets(store: IEventStore) {
    store.readModels.watch(ReleasingWatchSupportTicket::class).collect { changeset ->
        if (changeset.removed || changeset.readModel == null) return@collect

        val ticket = store.readModels.release(changeset.readModel!!)
        println("${changeset.modelKey}: ${ticket.requesterName}")
    }
}
```
