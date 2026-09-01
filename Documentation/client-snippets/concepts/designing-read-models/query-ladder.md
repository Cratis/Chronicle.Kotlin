```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
data class DesigningReadModelsCustomerListItem(val id: String = "", val name: String = "")

// Every instance in one call — read from the materialized store
suspend fun getEveryInstance(store: IEventStore) =
    store.readModels.getInstances(DesigningReadModelsCustomerListItem::class)

// One page of materialized instances, with paging done by the store
suspend fun getPage(store: IEventStore) =
    store.readModels.materialized.getInstances(DesigningReadModelsCustomerListItem::class, skip = 0, take = 20)
```
