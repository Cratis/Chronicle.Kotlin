```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.events.EventType

@EventType
data class SubjectShippingAddressChanged(val street: String)

class SubjectShippingService(private val eventStore: IEventStore) {
    suspend fun changeAddress(orderId: String, customerId: String, street: String) {
        // The event happens to the order, but the address is the customer's data - so the
        // subject is the customer, not the event source.
        eventStore.eventLog.append(
            orderId,
            SubjectShippingAddressChanged(street),
            AppendOptions(subject = customerId)
        )
    }
}
```
