```kotlin
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.projections.SetFromContext
import io.cratis.chronicle.projections.SetValue
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
@FromEvent(GetStartedBookAdded::class)
data class GetStartedBookDetails(
    @SetFromContext(eventType = GetStartedBookAdded::class, contextProperty = "eventSourceId")
    val id: String = "",
    val title: String = "",
    val isbn: String = "",
    @SetValue(eventType = GetStartedBookAdded::class, value = "false")
    @SetValue(eventType = GetStartedBookBorrowed::class, value = "true")
    @SetValue(eventType = GetStartedBookReturned::class, value = "false")
    val onLoan: Boolean = false,
    @SetFrom(propertyPath = "memberName", eventType = GetStartedBookBorrowed::class)
    @SetValue(eventType = GetStartedBookReturned::class, clear = true)
    val borrowedBy: String? = null,
)
```
