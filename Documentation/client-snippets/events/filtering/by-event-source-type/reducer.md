```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.observation.EventSourceType
import io.cratis.chronicle.observation.Reducer

@EventType
data class FilterBySourceTypeInvoiceIssued(val amount: Double = 0.0)

data class FilterBySourceTypeCustomerInvoiceTotal(val amount: Double = 0.0)

@EventSourceType("customer")
@Reducer
class FilterBySourceTypeCustomerInvoiceTotalReducer {
    fun invoiceIssued(event: FilterBySourceTypeInvoiceIssued, current: FilterBySourceTypeCustomerInvoiceTotal?): FilterBySourceTypeCustomerInvoiceTotal =
        FilterBySourceTypeCustomerInvoiceTotal((current?.amount ?: 0.0) + event.amount)
}

suspend fun issueCustomerInvoice(store: IEventStore, eventSourceId: String, amount: Double) =
    store.eventLog.append(eventSourceId, FilterBySourceTypeInvoiceIssued(amount), AppendOptions(eventSourceType = "customer"))
```
