```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.observation.EventStreamType
import io.cratis.chronicle.observation.Reactor

@EventType
data class FilterByStreamTypePaymentCaptured(val amount: Double = 0.0)

suspend fun capture(store: IEventStore, eventSourceId: String, amount: Double) =
    store.eventLog.append(eventSourceId, FilterByStreamTypePaymentCaptured(amount), AppendOptions(eventStreamType = "payments"))

@EventStreamType("payments")
@Reactor
class FilterByStreamTypePaymentNotificationsReactor {
    fun paymentCaptured(event: FilterByStreamTypePaymentCaptured, context: EventContext) {
        // Only handles events appended to the "payments" stream type
    }
}
```
