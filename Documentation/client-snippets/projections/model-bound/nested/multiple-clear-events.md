```kotlin title="Clear a nested object from multiple events"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.ClearWith
import io.cratis.chronicle.projections.FromEvent

@EventType
data class CommandSetForNestedMultipleClear(val name: String, val schema: String)

@EventType
data class CommandClearedForNestedMultipleClear(val placeholder: Boolean = true)

@EventType
data class SliceArchivedForNestedMultipleClear(val placeholder: Boolean = true)

@FromEvent(CommandSetForNestedMultipleClear::class)
@ClearWith(CommandClearedForNestedMultipleClear::class)
@ClearWith(SliceArchivedForNestedMultipleClear::class)
data class CommandItemNestedMultipleClear(
    val name: String = "",
    val schema: String = ""
)
```
