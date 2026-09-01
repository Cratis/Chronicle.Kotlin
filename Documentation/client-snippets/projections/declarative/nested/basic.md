```kotlin title="Nested object projection"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

@EventType(id = "slice-created-for-nested-basic")
data class SliceCreatedForNestedBasic(val name: String)

@EventType(id = "command-set-for-declarative-nested-basic")
data class CommandSetForDeclarativeNestedBasic(val name: String, val schema: String)

@EventType(id = "command-cleared-for-declarative-nested-basic")
data class CommandClearedForDeclarativeNestedBasic(val placeholder: Boolean = true)

data class SliceForNestedBasic(
    val name: String = "",
    val command: CommandItemForNestedBasic? = null
)

data class CommandItemForNestedBasic(
    val name: String = "",
    val schema: String = ""
)

class SliceProjectionForNestedBasic : IProjectionFor<SliceForNestedBasic> {
    override fun define(builder: IProjectionBuilderFor<SliceForNestedBasic>) {
        builder
            .from(SliceCreatedForNestedBasic::class)
            .nested(SliceForNestedBasic::command, CommandItemForNestedBasic::class) { nested ->
                nested
                    .from(CommandSetForDeclarativeNestedBasic::class)
                    .clearWith(CommandClearedForDeclarativeNestedBasic::class)
            }
    }
}
```
