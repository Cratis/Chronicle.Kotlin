```kotlin title="Explicit mappings on a nested type"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.ClearWith
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.SetFrom

@EventType(id = "command-set-for-nested-explicit")
data class CommandSetForNestedExplicit(val commandName: String, val jsonSchema: String)

@EventType(id = "command-schema-updated-for-nested-explicit")
data class CommandSchemaUpdatedForNestedExplicit(val updatedSchema: String)

@EventType(id = "command-cleared-for-nested-explicit")
data class CommandClearedForNestedExplicit(val placeholder: Boolean = true)

@FromEvent(CommandSetForNestedExplicit::class)
@FromEvent(CommandSchemaUpdatedForNestedExplicit::class)
@ClearWith(CommandClearedForNestedExplicit::class)
data class CommandItemNestedExplicit(
    @SetFrom("commandName", CommandSetForNestedExplicit::class)
    val name: String = "",

    @SetFrom("jsonSchema", CommandSetForNestedExplicit::class)
    @SetFrom("updatedSchema", CommandSchemaUpdatedForNestedExplicit::class)
    val schema: String = ""
)
```
