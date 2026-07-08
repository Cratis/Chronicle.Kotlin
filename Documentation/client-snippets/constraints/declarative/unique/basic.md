```kotlin
import io.cratis.chronicle.constraints.IConstraint
import io.cratis.chronicle.constraints.IConstraintBuilder
import io.cratis.chronicle.events.EventType

@EventType(id = "constraints-unique-basic-project-created")
data class ConstraintsUniqueBasicProjectCreated(val name: String)

class ConstraintsUniqueBasicProjectName : IConstraint {
    override fun define(builder: IConstraintBuilder) {
        builder.unique { unique ->
            unique.on(ConstraintsUniqueBasicProjectCreated::class) { it.name }
        }
    }
}
```
