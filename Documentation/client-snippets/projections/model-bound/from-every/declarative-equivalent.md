```kotlin title="Declarative projection with every-event metadata"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

@EventType(id = "inventory-registered-declarative-for-every")
data class InventoryRegisteredDeclarativeForEvery(val productName: String)

@EventType(id = "inventory-adjusted-declarative-for-every")
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
