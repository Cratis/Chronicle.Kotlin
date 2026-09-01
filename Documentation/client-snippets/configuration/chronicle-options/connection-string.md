```kotlin
import io.cratis.chronicle.ChronicleOptions
import io.cratis.chronicle.connection.ChronicleConnectionString

fun optionsFromConnectionString(): ChronicleOptions =
    ChronicleOptions(connectionString = ChronicleConnectionString.parse("chronicle://myserver:35000"))
```
