```kotlin
import io.cratis.chronicle.ChronicleOptions

fun optionsWithLargerMessages(): ChronicleOptions =
    ChronicleOptions.fromConnectionString("chronicle://localhost:35000")
        .withMaxReceiveMessageSize(200 * 1024 * 1024) // 200 MB
```
