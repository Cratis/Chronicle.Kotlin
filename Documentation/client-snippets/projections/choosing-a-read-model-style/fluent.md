```kotlin
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

data class ChoosingStyleBookStatusFluent(
    val id: String = "",
    val title: String = "",
    val isbn: String = "",
    val isBorrowed: Boolean = false,
    val borrowedBy: String? = null
)

class ChoosingStyleBookStatusProjection : IProjectionFor<ChoosingStyleBookStatusFluent> {
    override fun define(builder: IProjectionBuilderFor<ChoosingStyleBookStatusFluent>) {
        builder
            .from(ChoosingStyleBookRegistered::class) {
                it.set(ChoosingStyleBookStatusFluent::id).toEventSourceId()
                it.set(ChoosingStyleBookStatusFluent::title).to { e -> e.title }
                it.set(ChoosingStyleBookStatusFluent::isbn).to { e -> e.isbn }
                it.set(ChoosingStyleBookStatusFluent::isBorrowed).to { false }
                it.set(ChoosingStyleBookStatusFluent::borrowedBy).to { null }
            }
            .from(ChoosingStyleBookBorrowed::class) {
                it.set(ChoosingStyleBookStatusFluent::isBorrowed).to { true }
                it.set(ChoosingStyleBookStatusFluent::borrowedBy).to { e -> e.memberName }
            }
            .from(ChoosingStyleBookReturned::class) {
                it.set(ChoosingStyleBookStatusFluent::isBorrowed).to { false }
                it.set(ChoosingStyleBookStatusFluent::borrowedBy).to { null }
            }
    }
}
```
