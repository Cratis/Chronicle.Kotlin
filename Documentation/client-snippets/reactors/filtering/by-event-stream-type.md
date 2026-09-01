```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.IEventLog
import io.cratis.chronicle.observation.EventStreamType
import io.cratis.chronicle.observation.Reactor

@EventType(id = "reactors-filtering-payment-captured")
data class ReactorsFilteringPaymentCaptured(val amount: Double)

class ReactorsFilteringPaymentsService(private val eventLog: IEventLog) {
    suspend fun capture(eventSourceId: String, amount: Double) =
        eventLog.append(
            eventSourceId,
            ReactorsFilteringPaymentCaptured(amount),
            AppendOptions(eventStreamType = "payments")
        )
}

@Reactor
@EventStreamType("payments")
class ReactorsFilteringPaymentReceivedNotifier {
    fun captured(event: ReactorsFilteringPaymentCaptured, context: EventContext) {
    }
}
```
