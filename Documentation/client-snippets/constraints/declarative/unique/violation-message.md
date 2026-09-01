```kotlin
import io.cratis.chronicle.constraints.IConstraint
import io.cratis.chronicle.constraints.IConstraintBuilder
import io.cratis.chronicle.events.EventType

@EventType
data class ConstraintsUniqueMessageProjectCreated(val name: String)

class ConstraintsUniqueMessageProjectName : IConstraint {
    override fun define(builder: IConstraintBuilder) {
        builder.unique { unique ->
            unique
                .on(ConstraintsUniqueMessageProjectCreated::class, ConstraintsUniqueMessageProjectCreated::name)
                .withMessage("A project with this name already exists.")
        }
    }
}
```
