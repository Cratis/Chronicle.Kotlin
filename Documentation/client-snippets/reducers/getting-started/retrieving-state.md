```kotlin
import io.cratis.chronicle.IEventStore

class ReducersGettingStartedOrderService(private val store: IEventStore) {
    suspend fun getOrderSummary(orderId: String): ReducersGettingStartedOrderSummary? =
        store.readModels.getInstanceByKey(ReducersGettingStartedOrderSummary::class, orderId)
}
```
