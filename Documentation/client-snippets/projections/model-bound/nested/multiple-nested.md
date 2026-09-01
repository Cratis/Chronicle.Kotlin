```kotlin title="Multiple nested objects on one parent"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.ClearWith
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.Nested
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class SliceCreatedForNestedMultiple(val name: String)

@EventType
data class CommandSetForNestedMultiple(val name: String, val schema: String)

@EventType
data class CommandClearedForNestedMultiple(val placeholder: Boolean = true)

@EventType
data class ValidationConfiguredForNestedMultiple(val rules: String, val isStrict: Boolean)

@EventType
data class ValidationRemovedForNestedMultiple(val placeholder: Boolean = true)

@ReadModel
@FromEvent(SliceCreatedForNestedMultiple::class)
data class SliceWithMultipleNestedObjects(
    val name: String = "",

    @Nested
    val command: CommandItemNestedMultiple? = null,

    @Nested
    val validation: ValidationConfigNestedMultiple? = null
)

@FromEvent(CommandSetForNestedMultiple::class)
@ClearWith(CommandClearedForNestedMultiple::class)
data class CommandItemNestedMultiple(val name: String = "", val schema: String = "")

@FromEvent(ValidationConfiguredForNestedMultiple::class)
@ClearWith(ValidationRemovedForNestedMultiple::class)
data class ValidationConfigNestedMultiple(val rules: String = "", val isStrict: Boolean = false)
```
