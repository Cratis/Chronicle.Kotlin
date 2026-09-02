```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.OperationContext
import io.cratis.chronicle.transactions.UnitOfWork

data class TransactionalOrderPlaced(val orderId: String, val totalAmount: Double)
data class TransactionalInventoryReserved(val sku: String, val quantity: Int)

suspend fun commitOrder(store: IEventStore, context: OperationContext) {
    val transaction = UnitOfWork(store.eventLog, context)
    try {
        transaction.append("order-123", TransactionalOrderPlaced("order-123", 99.95))
        transaction.append("inventory-widget", TransactionalInventoryReserved("widget", 1))
        transaction.commit()
    } catch (exception: Exception) {
        if (!transaction.isCompleted) transaction.rollback()
        throw exception
    }
}
```
