```kotlin
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.RemovedWith
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
@FromEvent(GetStartedBookBorrowed::class)
@RemovedWith(GetStartedBookReturned::class)
data class GetStartedBorrowedBook(
    val id: String = "",
    val memberName: String = ""
)
```
