```kotlin title="Disable AutoMap on a nested type"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.ClearWith
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.NoAutoMap
import io.cratis.chronicle.projections.SetFrom

@EventType
data class CommandSetForNestedNoAutoMap(val commandName: String, val schema: String)

@EventType
data class CommandClearedForNestedNoAutoMap(val placeholder: Boolean = true)

@FromEvent(CommandSetForNestedNoAutoMap::class)
@ClearWith(CommandClearedForNestedNoAutoMap::class)
@NoAutoMap
data class CommandItemNestedNoAutoMap(
    @SetFrom("commandName", CommandSetForNestedNoAutoMap::class)
    val name: String = "",

    @SetFrom("schema", CommandSetForNestedNoAutoMap::class)
    val schema: String = ""
)
```
