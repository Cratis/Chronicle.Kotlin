```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.events.EventType

@EventType
data class TransactionalOrderPlaced(val orderId: String, val totalAmount: Double)
@EventType
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

        // Consecutive events for the same event source and options append atomically;
        // the unit of work is not atomic across event sources.
        unitOfWork.commit()
        check(unitOfWork.isSuccess) { "Commit failed: ${unitOfWork.getConstraintViolations()}, ${unitOfWork.getConcurrencyViolations()}, ${unitOfWork.getAppendErrors()}" }
    } catch (exception: Exception) {
        if (!unitOfWork.isCompleted) unitOfWork.rollback()
        throw exception
    }
}
```
