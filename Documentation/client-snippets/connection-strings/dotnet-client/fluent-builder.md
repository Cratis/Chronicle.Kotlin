```kotlin
import io.cratis.chronicle.ChronicleClient
import io.cratis.chronicle.ChronicleOptions
import io.cratis.chronicle.connection.ChronicleConnectionString
import io.cratis.chronicle.connection.ChronicleServerAddress

// Kotlin has no separate connection string builder type — a data class with named constructor
// arguments already gives every property a name, so ChronicleConnectionString is constructed
// directly rather than through a fluent builder.
fun createClientWithExplicitConnectionString(): ChronicleClient {
    val connectionString = ChronicleConnectionString(
        addresses = listOf(ChronicleServerAddress("server.example.com", 35000)),
        username = "clientId",
        password = "clientSecret"
    )
    return ChronicleClient(ChronicleOptions(connectionString))
}
```
