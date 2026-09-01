```kotlin
import io.cratis.chronicle.ChronicleClient
import io.cratis.chronicle.ChronicleOptions
import io.cratis.chronicle.connection.ChronicleConnectionString

fun createFromOptions(): ChronicleClient = ChronicleClient(ChronicleOptions.development())

fun createFromConnectionString(): ChronicleClient = ChronicleClient(ChronicleOptions(ChronicleConnectionString.DEVELOPMENT))
```
