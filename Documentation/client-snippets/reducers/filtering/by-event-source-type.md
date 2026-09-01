```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.IEventLog
import io.cratis.chronicle.observation.EventSourceType
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.readModels.ReadModel

@EventType(id = "reducers-filtering-invoice-issued")
data class ReducersFilteringInvoiceIssued(val amount: Double)

@ReadModel
data class ReducersFilteringCustomerInvoiceTotal(val amount: Double = 0.0)

class ReducersFilteringInvoicingService(private val eventLog: IEventLog) {
    suspend fun issueCustomerInvoice(eventSourceId: String, amount: Double) =
        eventLog.append(
            eventSourceId,
            ReducersFilteringInvoiceIssued(amount),
            AppendOptions(eventSourceType = "customer")
        )
}

@Reducer
@EventSourceType("customer")
class ReducersFilteringCustomerInvoiceTotalReducer {
    fun issued(event: ReducersFilteringInvoiceIssued, current: ReducersFilteringCustomerInvoiceTotal?, context: EventContext) =
        ReducersFilteringCustomerInvoiceTotal((current?.amount ?: 0.0) + event.amount)
}
```
