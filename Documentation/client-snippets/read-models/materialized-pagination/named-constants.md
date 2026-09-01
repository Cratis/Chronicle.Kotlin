```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
data class NamedConstantsOrder(val status: String = "", val total: Double = 0.0)

/**
 * `skip` and `take` both default to well-known values (0 and 50) - call without arguments to
 * use them rather than repeating the numbers yourself.
 */
suspend fun getOrders(store: IEventStore): List<NamedConstantsOrder> =
    store.readModels.materialized.getInstances(NamedConstantsOrder::class)
```
