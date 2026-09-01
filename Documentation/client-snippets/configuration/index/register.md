```kotlin
import io.cratis.chronicle.ChronicleClient
import io.cratis.chronicle.ChronicleOptions
import io.cratis.chronicle.EventStore
import io.cratis.chronicle.connection.ChronicleConnectionString

fun registerEventStore(): EventStore {
    val options = ChronicleOptions(connectionString = ChronicleConnectionString.DEVELOPMENT)
    val client = ChronicleClient(options)
    return client.getEventStore("my-store")
}
```
