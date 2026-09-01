```kotlin
import io.cratis.chronicle.ChronicleOptions
import io.cratis.chronicle.connection.ChronicleConnectionString
import io.cratis.chronicle.sinks.WellKnownSinkTypes

fun optionsWithSqlSink(): ChronicleOptions = ChronicleOptions(
    connectionString = ChronicleConnectionString.DEVELOPMENT,
    defaultSinkTypeId = WellKnownSinkTypes.SQL
)
```
