```kotlin
import io.cratis.chronicle.ChronicleClient
import io.cratis.chronicle.ChronicleOptions
import io.cratis.chronicle.EventStore

// The Kotlin client has no separate compliance registration step. Every event type and read model
// it registers carries its @Pii metadata in the schema it sends to the kernel, so connecting the
// client is all compliance needs - and the compliance service is on every event store it returns.
object ComplianceClientRegistration {
    fun configure(): EventStore {
        val client = ChronicleClient(ChronicleOptions.development())
        return client.getEventStore("Sales")
    }
}
```
