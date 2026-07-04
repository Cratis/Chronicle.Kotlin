```kotlin
import io.cratis.chronicle.IEventStore

data class TransactionalOrderPlaced(val orderId: String, val totalAmount: Double)
data class TransactionalInventoryReserved(val sku: String, val quantity: Int)

suspend fun commitOrder(store: IEventStore) {
    val unitOfWork = store.unitOfWorkManager.begin()

    try {
        store.eventLog.transactional.append(
            "order-123",
            TransactionalOrderPlaced("order-123", 99.95)
        )

        store.eventLog.transactional.append(
            "inventory-widget",
            TransactionalInventoryReserved("widget", 1)
        )

        unitOfWork.commit()
    } catch (exception: Exception) {
        unitOfWork.rollback()
        throw exception
    }
}
```
