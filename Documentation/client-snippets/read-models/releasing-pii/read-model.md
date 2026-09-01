```kotlin
import io.cratis.chronicle.compliance.Pii
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class SupportTicketOpened(val customerId: String = "", val requesterName: String = "")

/**
 * [Release][io.cratis.chronicle.readModels.IReadModelsService.release] resolves whose encryption
 * key to use by looking for a property named `id`, case-insensitive - here that is the ticket's
 * own key, which is also the customer it belongs to.
 */
@ReadModel
data class ReleasingReadModelSupportTicket(
    val id: String = "",
    @Pii val requesterName: String = ""
)
```
