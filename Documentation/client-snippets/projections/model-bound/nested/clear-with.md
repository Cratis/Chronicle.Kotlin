```kotlin title="Clear a nested object"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.ClearWith
import io.cratis.chronicle.projections.FromEvent

@EventType(id = "command-set-for-nested-clear")
data class CommandSetForNestedClear(val name: String, val schema: String)

@EventType(id = "command-cleared-for-nested-clear")
data class CommandClearedForNestedClear(val placeholder: Boolean = true)

@FromEvent(CommandSetForNestedClear::class)
@ClearWith(CommandClearedForNestedClear::class)
data class CommandItemNestedClear(
    val name: String = "",
    val schema: String = ""
)
```
