```kotlin
import io.cratis.chronicle.IEventStore

data class DesigningReadModelsCustomerDetail(val id: String, val name: String)

class DesigningReadModelsCustomerDetailService(private val store: IEventStore) {
    suspend fun getDetail(customerId: String): DesigningReadModelsCustomerDetail? =
        store.readModels.getInstanceByKey(DesigningReadModelsCustomerDetail::class, customerId)
}
```
