```kotlin
import io.cratis.chronicle.constraints.IConstraint
import io.cratis.chronicle.constraints.IConstraintBuilder
import io.cratis.chronicle.events.EventType

@EventType(id = "constraints-unique-message-project-created")
data class ConstraintsUniqueMessageProjectCreated(val name: String)

class ConstraintsUniqueMessageProjectName : IConstraint {
    override fun define(builder: IConstraintBuilder) {
        builder.unique { unique ->
            unique
                .on(ConstraintsUniqueMessageProjectCreated::class) { it.name }
                .withMessage("A project with this name already exists.")
        }
    }
}
```
