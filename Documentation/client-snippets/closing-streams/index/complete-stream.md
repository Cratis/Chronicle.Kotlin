```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.eventSequences.CompleteStreamResult

/**
 * Closes the invoice stream so no further line items can be appended to it once the invoice
 * has been issued.
 */
suspend fun closeInvoiceStream(store: IEventStore, invoiceStreamId: String) {
    when (val result = store.eventLog.completeStream("invoices", invoiceStreamId)) {
        is CompleteStreamResult.Success -> println("Stream closed at sequence ${result.sequenceNumber.value}")
        CompleteStreamResult.AlreadyCompleted -> println("Failed to close stream: already completed")
        CompleteStreamResult.DefaultStreamCannotBeCompleted -> println("Failed to close stream: the default stream cannot be completed")
    }
}
```
