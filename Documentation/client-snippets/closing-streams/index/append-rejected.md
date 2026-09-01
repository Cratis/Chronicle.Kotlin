```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.eventSequences.AppendOptions

@EventType
data class ClosingStreamsInvoiceLineAdded(val description: String = "", val amount: Double = 0.0)

/**
 * Appends a line item to an invoice stream. Once the stream has been closed, the append is
 * rejected with a "StreamClosed" constraint violation and no further lines can be added.
 */
suspend fun tryAppendLine(store: IEventStore, invoiceId: String): Boolean {
    val result = store.eventLog.append(
        invoiceId,
        ClosingStreamsInvoiceLineAdded("Consulting", 500.0),
        AppendOptions(eventStreamType = "invoices", eventStreamId = "invoice-42")
    )

    if (!result.isSuccess) {
        return result.constraintViolations.none { it.constraintId == "StreamClosed" }
    }

    return true
}
```
