```kotlin
import io.cratis.chronicle.eventSequences.EventForEventSourceId
import io.cratis.chronicle.eventSequences.IEventLog
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.imports.ImportOptions
import io.cratis.chronicle.imports.ImportResult

@EventType
data class LegacyOrderPlaced(val orderNumber: String)

data class LegacyOrder(val orderId: String, val number: String)

class OrderMigration(private val eventLog: IEventLog) {
    suspend fun run(batches: Sequence<List<LegacyOrder>>): ImportResult {
        val import = eventLog.beginImport(ImportOptions(maxChunksInFlight = 4))
        println("Importing as ${import.importId}")
        try {
            for (batch in batches) {
                import.add(batch.map { order -> EventForEventSourceId(order.orderId, LegacyOrderPlaced(order.number)) })
            }
            return import.complete()
        } finally {
            import.close()
        }
    }
}
```
