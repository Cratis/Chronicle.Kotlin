```kotlin
import io.cratis.chronicle.eventSequences.EventForEventSourceId
import io.cratis.chronicle.eventSequences.IEventLog
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.imports.ImportOptions
import io.cratis.chronicle.imports.ImportResult
import io.cratis.chronicle.imports.OnInvalidEvent
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@EventType
data class LegacyCustomerRegistered(val name: String)

data class LegacyRow(val eventId: String, val customerId: String, val name: String, val at: Instant, val position: Long)

class CustomerMigration(private val eventLog: IEventLog) {
    // The rows are read as the import uploads them, so the source is never held in memory as a whole.
    suspend fun run(rows: Flow<LegacyRow>): ImportResult =
        eventLog.import(
            rows.map { row ->
                EventForEventSourceId(
                    eventSourceId = row.customerId,
                    event = LegacyCustomerRegistered(row.name),
                    occurred = row.at,
                    idempotencyKey = row.eventId,
                    sourceSequenceNumber = row.position,
                )
            },
            ImportOptions(onInvalid = OnInvalidEvent.Reject),
        ) { progress -> println("${progress.state}: ${progress.eventsCommitted} committed") }
}
```
