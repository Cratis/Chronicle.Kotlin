```kotlin title="Update a nested object from multiple events"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.ClearWith
import io.cratis.chronicle.projections.FromEvent

@EventType(id = "command-set-for-nested-multiple-from")
data class CommandSetForNestedMultipleFrom(val name: String, val schema: String)

@EventType(id = "command-renamed-for-nested-multiple-from")
data class CommandRenamedForNestedMultipleFrom(val name: String)

@EventType(id = "command-schema-updated-for-nested-multiple-from")
data class CommandSchemaUpdatedForNestedMultipleFrom(val schema: String)

@EventType(id = "command-cleared-for-nested-multiple-from")
data class CommandClearedForNestedMultipleFrom(val placeholder: Boolean = true)

@FromEvent(CommandSetForNestedMultipleFrom::class)
@FromEvent(CommandRenamedForNestedMultipleFrom::class)
@FromEvent(CommandSchemaUpdatedForNestedMultipleFrom::class)
@ClearWith(CommandClearedForNestedMultipleFrom::class)
data class CommandItemNestedMultipleFrom(
    val name: String = "",
    val schema: String = ""
)
```
