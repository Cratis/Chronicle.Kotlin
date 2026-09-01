```kotlin title="Complete nested object projection"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.ClearWith
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.Nested
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.readModels.ReadModel

@EventType(id = "slice-created-for-nested-complete")
data class SliceCreatedForNestedComplete(val name: String)

@EventType(id = "command-set-for-nested-complete")
data class CommandSetForNestedComplete(
    val commandId: String,
    val name: String,
    val schema: String,
    val rules: String,
    val stateSchema: String
)

@EventType(id = "command-renamed-for-nested-complete")
data class CommandRenamedForNestedComplete(val commandId: String, val name: String)

@EventType(id = "command-definition-updated-for-nested-complete")
data class CommandDefinitionUpdatedForNestedComplete(
    val commandId: String,
    val schema: String,
    val rules: String,
    val stateSchema: String
)

@EventType(id = "command-cleared-for-nested-complete")
data class CommandClearedForNestedComplete(val placeholder: Boolean = true)

@ReadModel
@FromEvent(SliceCreatedForNestedComplete::class)
data class SliceNestedComplete(
    val name: String = "",

    @Nested
    val command: CommandItemNestedComplete? = null
)

@FromEvent(CommandSetForNestedComplete::class)
@FromEvent(CommandRenamedForNestedComplete::class)
@FromEvent(CommandDefinitionUpdatedForNestedComplete::class)
@ClearWith(CommandClearedForNestedComplete::class)
data class CommandItemNestedComplete(
    @SetFrom("commandId", CommandSetForNestedComplete::class)
    val id: String = "",

    @SetFrom("name", CommandSetForNestedComplete::class)
    @SetFrom("name", CommandRenamedForNestedComplete::class)
    val name: String = "",

    @SetFrom("schema", CommandSetForNestedComplete::class)
    @SetFrom("schema", CommandDefinitionUpdatedForNestedComplete::class)
    val schema: String = "",

    @SetFrom("rules", CommandSetForNestedComplete::class)
    @SetFrom("rules", CommandDefinitionUpdatedForNestedComplete::class)
    val rules: String = "",

    @SetFrom("stateSchema", CommandSetForNestedComplete::class)
    @SetFrom("stateSchema", CommandDefinitionUpdatedForNestedComplete::class)
    val stateSchema: String = ""
)
```
