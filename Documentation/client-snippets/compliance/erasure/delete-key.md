```kotlin
import io.cratis.chronicle.ChronicleClient

object ComplianceErasureDeleteKey {
    suspend fun delete(chronicleClient: ChronicleClient) {
        val eventStore = chronicleClient.getEventStore("Sales")
        eventStore.compliance.deleteEncryptionKey("person-42")
    }
}
```
