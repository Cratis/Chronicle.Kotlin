```kotlin
import io.cratis.chronicle.observation.Reducer

data class ChoosingStyleBookStatusReducerModel(
    val title: String = "",
    val isbn: String = "",
    val isBorrowed: Boolean = false,
    val borrowedBy: String? = null
)

@Reducer
class ChoosingStyleBookStatusReducer {
    fun choosingStyleBookRegistered(event: ChoosingStyleBookRegistered): ChoosingStyleBookStatusReducerModel =
        ChoosingStyleBookStatusReducerModel(
            title = event.title,
            isbn = event.isbn,
            isBorrowed = false,
            borrowedBy = null
        )

    fun choosingStyleBookBorrowed(
        event: ChoosingStyleBookBorrowed,
        current: ChoosingStyleBookStatusReducerModel
    ): ChoosingStyleBookStatusReducerModel =
        current.copy(isBorrowed = true, borrowedBy = event.memberName)

    fun choosingStyleBookReturned(
        event: ChoosingStyleBookReturned,
        current: ChoosingStyleBookStatusReducerModel
    ): ChoosingStyleBookStatusReducerModel =
        current.copy(isBorrowed = false, borrowedBy = null)
}
```
