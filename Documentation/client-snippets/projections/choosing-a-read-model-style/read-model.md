```kotlin
import io.cratis.chronicle.compliance.Pii

data class ChoosingStyleBookStatus(
    val id: String = "",
    val title: String? = null,
    val isbn: String? = null,
    val isBorrowed: Boolean = false,
    @property:Pii val borrowedBy: String? = null
)
```
