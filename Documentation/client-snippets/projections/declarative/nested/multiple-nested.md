```kotlin title="Multiple nested objects"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

@EventType
data class SliceCreatedWithMultipleNested(val name: String)

@EventType
data class CommandSetWithMultipleNested(val name: String, val schema: String)

@EventType
data class CommandClearedWithMultipleNested(val placeholder: Boolean = true)

@EventType
data class ValidationConfiguredWithMultipleNested(val ruleName: String)

@EventType
data class ValidationRemovedWithMultipleNested(val placeholder: Boolean = true)

data class SliceWithMultipleNested(
    val name: String = "",
    val command: CommandItemWithMultipleNested? = null,
    val validation: ValidationConfigWithMultipleNested? = null
)

data class CommandItemWithMultipleNested(
    val name: String = "",
    val schema: String = ""
)

data class ValidationConfigWithMultipleNested(
    val ruleName: String = ""
)

class SliceProjectionWithMultipleNested : IProjectionFor<SliceWithMultipleNested> {
    override fun define(builder: IProjectionBuilderFor<SliceWithMultipleNested>) {
        builder
            .from(SliceCreatedWithMultipleNested::class)
            .nested(SliceWithMultipleNested::command, CommandItemWithMultipleNested::class) { nested ->
                nested
                    .from(CommandSetWithMultipleNested::class)
                    .clearWith(CommandClearedWithMultipleNested::class)
            }
            .nested(SliceWithMultipleNested::validation, ValidationConfigWithMultipleNested::class) { nested ->
                nested
                    .from(ValidationConfiguredWithMultipleNested::class)
                    .clearWith(ValidationRemovedWithMultipleNested::class)
            }
    }
}
```
