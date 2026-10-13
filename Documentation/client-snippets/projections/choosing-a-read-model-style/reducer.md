```kotlin
import io.cratis.chronicle.compliance.Pii
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.observation.Reducer

data class ChoosingStyleBookStatusReducerModel(
    val id: String,
    val title: String? = null,
    val isbn: String? = null,
    val isBorrowed: Boolean = false,
    @property:Pii val borrowedBy: String? = null
)

@Reducer
class ChoosingStyleBookStatusReducer {
    fun choosingStyleBookRegistered(
        event: ChoosingStyleBookRegistered,
        current: ChoosingStyleBookStatusReducerModel?,
        context: EventContext
    ): ChoosingStyleBookStatusReducerModel =
        ChoosingStyleBookStatusReducerModel(
            id = context.eventSourceId,
            title = event.title,
            isbn = event.isbn,
            isBorrowed = false,
            borrowedBy = null
        )

    fun choosingStyleBookBorrowed(
        event: ChoosingStyleBookBorrowed,
        current: ChoosingStyleBookStatusReducerModel?,
        context: EventContext
    ): ChoosingStyleBookStatusReducerModel =
        (current ?: ChoosingStyleBookStatusReducerModel(context.eventSourceId))
            .copy(isBorrowed = true, borrowedBy = event.memberName)

    fun choosingStyleBookReturned(
        event: ChoosingStyleBookReturned,
        current: ChoosingStyleBookStatusReducerModel?,
        context: EventContext
    ): ChoosingStyleBookStatusReducerModel =
        (current ?: ChoosingStyleBookStatusReducerModel(context.eventSourceId))
            .copy(isBorrowed = false, borrowedBy = null)
}
```
