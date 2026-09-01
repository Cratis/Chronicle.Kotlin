```kotlin
import io.cratis.chronicle.ChronicleClient
import io.cratis.chronicle.ChronicleOptions

fun createDevelopmentClient(): ChronicleClient {
    val options = ChronicleOptions.development()
    return ChronicleClient(options)
}
```
