```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.compliance.Pii
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
data class ReleasingManyCustomerProfile(
    val id: String = "",
    @Pii(description = "Customer email address") val email: String = ""
)

/**
 * Decrypts PII-annotated properties for a batch of read model instances in one call — the
 * compliance subject for each instance is derived from its own `id` property.
 */
suspend fun releaseCustomerEmails(store: IEventStore, profiles: List<ReleasingManyCustomerProfile>) =
    store.readModels.releaseMany(profiles)
```
