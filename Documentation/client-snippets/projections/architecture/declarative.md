```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

@EventType(id = "architecture-declarative-item-added")
data class ArchitectureDeclarativeItemAdded(val category: String)

data class ArchitectureDeclarativeSummary(
    val category: String = "",
    val count: Int = 0
)

class ArchitectureDeclarativeSummaryProjection : IProjectionFor<ArchitectureDeclarativeSummary> {
    override fun define(builder: IProjectionBuilderFor<ArchitectureDeclarativeSummary>) {
        builder
            .from(ArchitectureDeclarativeItemAdded::class) {
                it.usingKey("category")
                it.count(ArchitectureDeclarativeSummary::count)
            }
    }
}
```
