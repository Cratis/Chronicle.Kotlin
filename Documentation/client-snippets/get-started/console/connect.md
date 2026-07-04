```kotlin
import io.cratis.chronicle.ChronicleClient
import io.cratis.chronicle.ChronicleOptions

fun run() {
    // ChronicleOptions.development() points at the local dev kernel on chronicle://localhost:35000
    val client = ChronicleClient(ChronicleOptions.development())
    val eventStore = client.getEventStore("Quickstart")
    println("Connected to event store: ${eventStore.name}")

    // Use eventStore for the lifetime of your program — appending, querying, and so on.
}
```
