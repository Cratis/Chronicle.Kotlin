```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

@EventType(id = "dec-functions-item-added")
data class DecFunctionsItemAdded(val name: String)

@EventType(id = "dec-functions-item-removed")
data class DecFunctionsItemRemoved(val name: String)

data class DecFunctionsInventory(val quantity: Int = 0)

class DecFunctionsInventoryProjection : IProjectionFor<DecFunctionsInventory> {
    override fun define(builder: IProjectionBuilderFor<DecFunctionsInventory>) {
        builder
            .autoMap()
            .from(DecFunctionsItemAdded::class) {
                it.increment(DecFunctionsInventory::quantity)
            }
            .from(DecFunctionsItemRemoved::class) {
                it.decrement(DecFunctionsInventory::quantity)
            }
    }
}
```
