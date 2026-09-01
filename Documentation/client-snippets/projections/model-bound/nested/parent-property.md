```kotlin title="Nested property on the parent"
import io.cratis.chronicle.projections.Nested

data class ParentWithNestedProperty(
    @Nested
    val child: NestedPropertyChild? = null
)

data class NestedPropertyChild(
    val name: String = "",
    val description: String = ""
)
```
