```kotlin
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.projections.SetValue
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
@FromEvent(BookRegistered::class)
@FromEvent(BookBorrowed::class)
@FromEvent(BookReturned::class)
data class BookStatus(
    val id: String = "",

    @SetFrom(eventType = BookRegistered::class) val title: String = "",
    @SetFrom(eventType = BookRegistered::class) val isbn: String = "",

    @SetValue(BookBorrowed::class, value = "true")
    @SetValue(BookReturned::class, value = "false")
    val isBorrowed: Boolean = false,

    @SetFrom("memberName", BookBorrowed::class)
    @SetValue(BookReturned::class, clear = true)
    val borrowedBy: String? = null
)
```
