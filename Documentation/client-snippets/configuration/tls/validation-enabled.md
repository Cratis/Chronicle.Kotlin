```kotlin
import io.cratis.chronicle.ChronicleOptions

fun optionsWithTlsValidationEnabled(): ChronicleOptions =
    ChronicleOptions.fromConnectionString("chronicle://my-server:35000?skipTlsValidation=false")
```
