```kotlin
import io.cratis.chronicle.ChronicleClient
import io.cratis.chronicle.ChronicleOptions

// Kotlin has no appsettings.json-style configuration binding — read the connection string from
// wherever the application keeps its configuration (here, an environment variable) and pass it
// straight to the client.
fun createClientFromEnvironment(): ChronicleClient {
    val connectionString = System.getenv("CHRONICLE_CONNECTION_STRING") ?: "chronicle://localhost:35000"
    return ChronicleClient(ChronicleOptions.fromConnectionString(connectionString))
}
```
