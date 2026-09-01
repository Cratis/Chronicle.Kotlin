```kotlin
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.RemovedWith
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
@FromEvent(BookBorrowed::class)
@RemovedWith(BookReturned::class)
data class BorrowedBook(
    val id: String = "",
    val memberName: String = ""
)
```
