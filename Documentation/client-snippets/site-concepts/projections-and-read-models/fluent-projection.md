```kotlin
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

class BookStatusProjection : IProjectionFor<BookStatus> {
    override fun define(builder: IProjectionBuilderFor<BookStatus>) {
        builder
            .from(BookRegistered::class) {
                it.set(BookStatus::id).toEventSourceId()
                it.set(BookStatus::isBorrowed).toValue(false)
                it.set(BookStatus::borrowedBy).toValue(null)
            }
            .from(BookBorrowed::class) {
                it.set(BookStatus::isBorrowed).toValue(true)
                it.set(BookStatus::borrowedBy).toProperty("memberName")
            }
            .from(BookReturned::class) {
                it.set(BookStatus::isBorrowed).toValue(false)
                it.set(BookStatus::borrowedBy).toValue(null)
            }
    }
}
```
