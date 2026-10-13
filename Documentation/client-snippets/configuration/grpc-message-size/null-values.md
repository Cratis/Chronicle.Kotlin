```kotlin
import io.cratis.chronicle.ChronicleOptions

fun optionsWithGrpcDefaults(): ChronicleOptions =
    ChronicleOptions.fromConnectionString("chronicle://localhost:35000")
        .withMaxReceiveMessageSize(null) // Uses gRPC default of 4 MB - not recommended
```
