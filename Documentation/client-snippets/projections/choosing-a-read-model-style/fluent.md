```kotlin
// Requires io.cratis:chronicle 6.5.0 or later.
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

class ChoosingStyleBookStatusProjection : IProjectionFor<ChoosingStyleBookStatus> {
    override fun define(builder: IProjectionBuilderFor<ChoosingStyleBookStatus>) {
        builder
            .from(ChoosingStyleBookRegistered::class) {
                it.set(ChoosingStyleBookStatus::id).toEventSourceId()
                it.set(ChoosingStyleBookStatus::title).toProperty("title")
                it.set(ChoosingStyleBookStatus::isbn).toProperty("isbn")
                it.set(ChoosingStyleBookStatus::isBorrowed).toValue(false)
                it.set(ChoosingStyleBookStatus::borrowedBy).toValue(null)
            }
            .from(ChoosingStyleBookBorrowed::class) {
                it.set(ChoosingStyleBookStatus::isBorrowed).toValue(true)
                it.set(ChoosingStyleBookStatus::borrowedBy).toProperty("memberName")
            }
            .from(ChoosingStyleBookReturned::class) {
                it.set(ChoosingStyleBookStatus::isBorrowed).toValue(false)
                it.set(ChoosingStyleBookStatus::borrowedBy).toValue(null)
            }
    }
}
```
