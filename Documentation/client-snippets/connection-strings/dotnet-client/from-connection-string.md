```kotlin
import io.cratis.chronicle.ChronicleClient
import io.cratis.chronicle.ChronicleOptions

fun createClientFromConnectionString(): ChronicleClient {
    val options = ChronicleOptions.fromConnectionString("chronicle://localhost:35000")
    return ChronicleClient(options)
}
```
