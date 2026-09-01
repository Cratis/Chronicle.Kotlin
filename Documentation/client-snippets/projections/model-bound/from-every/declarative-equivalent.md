```kotlin title="Declarative projection with every-event metadata"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

@EventType
data class InventoryRegisteredDeclarativeForEvery(val productName: String)

@EventType
data class InventoryAdjustedDeclarativeForEvery(val quantity: Int)

data class InventoryStatusDeclarativeFromEvery(
    val productName: String = "",
    val lastUpdated: String = ""
)

class InventoryStatusDeclarativeProjection : IProjectionFor<InventoryStatusDeclarativeFromEvery> {
    override fun define(builder: IProjectionBuilderFor<InventoryStatusDeclarativeFromEvery>) {
        builder
            .from(InventoryRegisteredDeclarativeForEvery::class)
            .from(InventoryAdjustedDeclarativeForEvery::class)
            .fromEvery { it ->
                it.set(InventoryStatusDeclarativeFromEvery::lastUpdated).toEventContextProperty("occurred")
            }
    }
}
```
