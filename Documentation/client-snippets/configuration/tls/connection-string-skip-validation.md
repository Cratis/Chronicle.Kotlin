```kotlin
import io.cratis.chronicle.ChronicleOptions

fun optionsSkippingTlsValidation(): ChronicleOptions =
    ChronicleOptions.fromConnectionString("chronicle://localhost:35000?skipTlsValidation=true")
```
