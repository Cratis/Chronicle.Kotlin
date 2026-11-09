```kotlin
import io.cratis.chronicle.eventSequences.EventForEventSourceId
import io.cratis.chronicle.eventSequences.IEventLog
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.imports.ImportId
import io.cratis.chronicle.imports.ImportResult

@EventType
data class LegacyInvoiceIssued(val number: String)

data class LegacyInvoice(val invoiceId: String, val number: String)

class InvoiceMigration(private val eventLog: IEventLog) {
    suspend fun resume(importId: ImportId, source: List<LegacyInvoice>): ImportResult {
        val import = eventLog.beginImport(importId)
        try {
            // The kernel already holds the first eventsAcknowledged events; continue with the rest.
            val remaining = source
                .drop(import.eventsAcknowledged.toInt())
                .map { invoice -> EventForEventSourceId(invoice.invoiceId, LegacyInvoiceIssued(invoice.number)) }
            import.add(remaining)
            return import.complete()
        } finally {
            import.close()
        }
    }
}
```
