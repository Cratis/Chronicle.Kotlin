```kotlin
import io.cratis.chronicle.ChronicleOptions
import io.cratis.chronicle.connection.ChronicleConnectionString

fun optionsWithCustomArtifacts(): ChronicleOptions = ChronicleOptions(
    connectionString = ChronicleConnectionString.DEVELOPMENT,
    artifacts = StructuralDepsMyArtifacts()
)
```
