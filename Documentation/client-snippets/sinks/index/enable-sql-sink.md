```kotlin
import io.cratis.chronicle.ChronicleOptions
import io.cratis.chronicle.connection.ChronicleConnectionString
import io.cratis.chronicle.sinks.WellKnownSinkTypes

fun optionsWithSqlSinkEnabled(): ChronicleOptions = ChronicleOptions(
    connectionString = ChronicleConnectionString.parse("chronicle://localhost:35000"),
    defaultSinkTypeId = WellKnownSinkTypes.SQL
)
```
