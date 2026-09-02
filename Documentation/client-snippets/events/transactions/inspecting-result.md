```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.OperationContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.transactions.UnitOfWork

@EventType
data class InspectingResultOrderPlaced(val orderId: String = "", val totalAmount: Double = 0.0)

suspend fun commitAndInspect(store: IEventStore, orderId: String, context: OperationContext) {
    val transaction = UnitOfWork(store.eventLog, context)
    transaction.append(orderId, InspectingResultOrderPlaced(orderId, 42.0))
    transaction.commit()

    if (transaction.isSuccess) {
        println("Committed up to sequence ${transaction.tryGetLastCommittedEventSequenceNumber()?.value}")
    } else {
        println("Failed: ${transaction.getAppendErrors().joinToString { it.message }}")
    }
}
```
