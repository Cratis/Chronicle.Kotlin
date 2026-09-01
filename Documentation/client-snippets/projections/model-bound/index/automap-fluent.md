```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

@EventType
data class IndexAutoMapAccountOpened(
    val name: String,
    val balance: Double
)

data class IndexAutoMapAccountInfo(
    val name: String = "",
    val balance: Double = 0.0
)

class IndexAutoMapAccountProjection : IProjectionFor<IndexAutoMapAccountInfo> {
    override fun define(builder: IProjectionBuilderFor<IndexAutoMapAccountInfo>) {
        // No configure block — matching properties are mapped automatically.
        builder.from(IndexAutoMapAccountOpened::class)
    }
}
```
