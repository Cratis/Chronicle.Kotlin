```kotlin
import io.cratis.chronicle.compliance.Pii
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.projections.SetFromContext
import io.cratis.chronicle.projections.SetValue

@FromEvent(ChoosingStyleBookRegistered::class)
@FromEvent(ChoosingStyleBookBorrowed::class)
@FromEvent(ChoosingStyleBookReturned::class)
data class ChoosingStyleBookStatusModelBound(
    @SetFromContext(ChoosingStyleBookRegistered::class, "eventSourceId")
    @SetFromContext(ChoosingStyleBookBorrowed::class, "eventSourceId")
    @SetFromContext(ChoosingStyleBookReturned::class, "eventSourceId")
    val id: String = "",

    @SetFrom("title", ChoosingStyleBookRegistered::class)
    val title: String? = null,

    @SetFrom("isbn", ChoosingStyleBookRegistered::class)
    val isbn: String? = null,

    @SetValue(ChoosingStyleBookRegistered::class, value = "false")
    @SetValue(ChoosingStyleBookBorrowed::class, value = "true")
    @SetValue(ChoosingStyleBookReturned::class, value = "false")
    val isBorrowed: Boolean = false,

    @property:Pii
    @SetFrom("memberName", ChoosingStyleBookBorrowed::class)
    @SetValue(ChoosingStyleBookRegistered::class, clear = true)
    @SetValue(ChoosingStyleBookReturned::class, clear = true)
    val borrowedBy: String? = null
)
```
