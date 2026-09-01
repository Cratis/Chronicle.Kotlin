```kotlin title="Fluent FromAll mapping"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor
import java.time.Instant

@EventType(id = "inventory-registered-from-all")
data class InventoryRegisteredFromAll(val productName: String)

@EventType(id = "inventory-adjusted-from-all")
data class InventoryAdjustedFromAll(val quantity: Int)

data class InventoryStatusFromAll(
    val productName: String = "",
    val lastUpdated: Instant = Instant.EPOCH
)

class InventoryStatusFromAllProjection : IProjectionFor<InventoryStatusFromAll> {
    override fun define(builder: IProjectionBuilderFor<InventoryStatusFromAll>) {
        builder
            .from(InventoryRegisteredFromAll::class)
            .from(InventoryAdjustedFromAll::class)
            .fromAll { it.set(InventoryStatusFromAll::lastUpdated).toEventContextProperty("occurred") }
    }
}
```
