```kotlin
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.projections.SetFromContext
import io.cratis.chronicle.projections.SetValue
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
@FromEvent(BookAdded::class)
data class TutorialBookDetails(
    @SetFromContext(eventType = BookAdded::class, contextProperty = "eventSourceId")
    val id: String = "",
    val title: String = "",
    val isbn: String = "",
    @SetValue(eventType = BookAdded::class, value = "false")
    @SetValue(eventType = BookBorrowed::class, value = "true")
    @SetValue(eventType = BookReturned::class, value = "false")
    val onLoan: Boolean = false,
    @SetFrom(propertyPath = "memberName", eventType = BookBorrowed::class)
    @SetValue(eventType = BookReturned::class, clear = true)
    val borrowedBy: String? = null,
)
```
