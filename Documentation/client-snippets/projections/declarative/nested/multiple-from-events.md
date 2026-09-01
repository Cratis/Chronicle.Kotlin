```kotlin title="Multiple nested events"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

@EventType
data class SliceCreatedForNestedUpdates(val name: String)

@EventType
data class CommandSetForNestedUpdates(val name: String, val schema: String)

@EventType
data class CommandRenamedForNestedUpdates(val newName: String)

@EventType
data class CommandSchemaUpdatedForNestedUpdates(val updatedSchema: String)

@EventType
data class CommandClearedForNestedUpdates(val placeholder: Boolean = true)

data class SliceForNestedUpdates(
    val name: String = "",
    val command: CommandItemForNestedUpdates? = null
)

data class CommandItemForNestedUpdates(
    val name: String = "",
    val schema: String = ""
)

class SliceProjectionForNestedUpdates : IProjectionFor<SliceForNestedUpdates> {
    override fun define(builder: IProjectionBuilderFor<SliceForNestedUpdates>) {
        builder
            .from(SliceCreatedForNestedUpdates::class)
            .nested(SliceForNestedUpdates::command, CommandItemForNestedUpdates::class) { nested ->
                nested
                    .from(CommandSetForNestedUpdates::class)
                    .from(CommandRenamedForNestedUpdates::class) {
                        it.set(CommandItemForNestedUpdates::name).to { e -> e.newName }
                    }
                    .from(CommandSchemaUpdatedForNestedUpdates::class) {
                        it.set(CommandItemForNestedUpdates::schema).to { e -> e.updatedSchema }
                    }
                    .clearWith(CommandClearedForNestedUpdates::class)
            }
    }
}
```
