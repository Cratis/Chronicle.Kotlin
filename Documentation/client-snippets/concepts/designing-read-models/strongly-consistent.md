```kotlin
// Requires io.cratis:chronicle 6.5.0 or later.
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.readModels.Passive
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class DesigningReadModelsCustomerNamed(val name: String)

// Nothing materializes this read model: every read computes it from the event log.
@Passive
@ReadModel
@FromEvent(DesigningReadModelsCustomerNamed::class)
data class DesigningReadModelsCustomerDetail(val name: String = "")

class DesigningReadModelsCustomerDetailService(private val eventStore: IEventStore) {
    suspend fun getDetail(customerId: String): DesigningReadModelsCustomerDetail? =
        eventStore.readModels.getInstanceByKey(DesigningReadModelsCustomerDetail::class, customerId)
}
```
