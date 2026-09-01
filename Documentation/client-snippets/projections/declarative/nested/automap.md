```kotlin title="AutoMap in a nested scope"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

@EventType(id = "slice-created-for-nested-automap")
data class SliceCreatedForNestedAutoMap(val name: String)

@EventType(id = "command-set-for-nested-automap")
data class CommandSetForNestedAutoMap(val name: String, val schema: String)

@EventType(id = "command-updated-for-nested-automap")
data class CommandUpdatedForNestedAutoMap(val schema: String)

@EventType(id = "command-cleared-for-nested-automap")
data class CommandClearedForNestedAutoMap(val placeholder: Boolean = true)

data class SliceForNestedAutoMap(
    val name: String = "",
    val command: CommandItemForNestedAutoMap? = null
)

data class CommandItemForNestedAutoMap(
    val name: String = "",
    val schema: String = ""
)

class SliceProjectionForNestedAutoMap : IProjectionFor<SliceForNestedAutoMap> {
    override fun define(builder: IProjectionBuilderFor<SliceForNestedAutoMap>) {
        builder
            .from(SliceCreatedForNestedAutoMap::class)
            .nested(SliceForNestedAutoMap::command, CommandItemForNestedAutoMap::class) { nested ->
                nested
                    .from(CommandSetForNestedAutoMap::class)
                    .from(CommandUpdatedForNestedAutoMap::class)
                    .clearWith(CommandClearedForNestedAutoMap::class)
            }
    }
}
```
