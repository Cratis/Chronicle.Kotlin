```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.OperationContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.transactions.UnitOfWork

@EventType
data class OrderedCustomerUpdated(val value: String = "")

suspend fun commitOrderedCustomerUpdates(store: IEventStore, context: OperationContext) {
    val transaction = UnitOfWork(store.eventLog, context)
    transaction.append("customer-1", OrderedCustomerUpdated("first"))
    transaction.append("customer-2", OrderedCustomerUpdated("second"))
    transaction.append("customer-1", OrderedCustomerUpdated("third"))
    transaction.commit() // one appendMany RPC, in exactly this order
}
```
