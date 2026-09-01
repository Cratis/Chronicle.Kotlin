```kotlin title="Nested object lifecycle"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.ClearWith
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.Nested
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class CommandSetForNestedBasic(val name: String, val schema: String)

@EventType
data class CommandClearedForNestedBasic(val placeholder: Boolean = true)

@ReadModel
@FromEvent(CommandSetForNestedBasic::class)
data class SliceWithNestedCommandBasic(
    val name: String = "",

    @Nested
    val command: CommandItemNestedBasic? = null
)

@FromEvent(CommandSetForNestedBasic::class)
@ClearWith(CommandClearedForNestedBasic::class)
data class CommandItemNestedBasic(
    val name: String = "",
    val schema: String = ""
)
```
