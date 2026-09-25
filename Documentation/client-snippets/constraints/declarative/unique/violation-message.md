```kotlin
import io.cratis.chronicle.constraints.Constraint
import io.cratis.chronicle.constraints.IConstraint
import io.cratis.chronicle.constraints.IConstraintBuilder
import io.cratis.chronicle.events.EventType

@EventType
data class ConstraintsUniqueMessageProjectCreated(val name: String)

@Constraint
class ConstraintsUniqueMessageProjectName : IConstraint {
    override fun define(builder: IConstraintBuilder) {
        builder.unique { unique ->
            unique
                .on(ConstraintsUniqueMessageProjectCreated::class, ConstraintsUniqueMessageProjectCreated::name)
                // Not sent to the kernel by io.cratis:chronicle 6.4.0; a violation carries the kernel's own message.
                .withMessage("A project with this name already exists.")
        }
    }
}
```
