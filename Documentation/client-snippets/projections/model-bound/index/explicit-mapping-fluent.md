```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

@EventType
data class IndexExplicitAccountOpened(
    val name: String,
    val initialBalance: Double
)

data class IndexExplicitAccountInfo(
    val name: String = "",
    val balance: Double = 0.0
)

class IndexExplicitAccountProjection : IProjectionFor<IndexExplicitAccountInfo> {
    override fun define(builder: IProjectionBuilderFor<IndexExplicitAccountInfo>) {
        builder.from(IndexExplicitAccountOpened::class) {
            it.set(IndexExplicitAccountInfo::name).toProperty("name")
            it.set(IndexExplicitAccountInfo::balance).toProperty("initialBalance")
        }
    }
}
```
