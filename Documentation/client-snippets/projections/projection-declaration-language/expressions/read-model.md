```kotlin
import java.time.Instant

class PdlExpressionsUserReadModel {
    var name: String = ""              // Requires string
    var loginCount: Int = 0            // Requires number
    var isActive: Boolean = false      // Requires boolean
    var createdAt: Instant = Instant.EPOCH // Requires timestamp
}
```
