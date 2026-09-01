```kotlin
import io.cratis.chronicle.readModels.ReadModel
import java.time.Instant

@ReadModel
data class CamelCasingUserReadModel(
    val firstName: String = "",
    val lastName: String = "",
    val emailAddress: String = "",
    val registrationDate: Instant = Instant.EPOCH
)
```
