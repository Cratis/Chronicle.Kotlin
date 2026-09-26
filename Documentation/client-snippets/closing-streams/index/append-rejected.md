```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.eventSequences.AppendOptions

@EventType
data class ClosingStreamsInvoiceLineAdded(val description: String = "", val amount: Double = 0.0)

/**
 * Appends a line item to an invoice stream. Once the stream has been closed, the append is
 * rejected with a "closed-stream" constraint violation and no further lines can be added.
 */
suspend fun tryAppendLine(store: IEventStore, invoiceId: String, eventStreamId: String): Boolean {
    val result = store.eventLog.append(
        invoiceId,
        ClosingStreamsInvoiceLineAdded("Consulting", 500.0),
        AppendOptions(eventStreamType = "invoices", eventStreamId = eventStreamId)
    )

    if (!result.isSuccess) {
        if (result.constraintViolations.any { it.constraintId == "closed-stream" }) return false
        throw IllegalStateException("Append failed: $result")
    }

    return true
}
```
