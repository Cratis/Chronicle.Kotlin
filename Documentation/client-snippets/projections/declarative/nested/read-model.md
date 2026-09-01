```kotlin title="Read model with nested object"
data class SliceWithNestedCommand(
    val name: String = "",
    val command: CommandItemForNestedCommand? = null
)

data class CommandItemForNestedCommand(
    val name: String = "",
    val schema: String = ""
)
```
