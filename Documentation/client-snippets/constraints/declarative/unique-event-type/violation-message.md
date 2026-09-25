```kotlin
// Requires io.cratis:chronicle 6.5.0 or later.
import io.cratis.chronicle.constraints.Constraint
import io.cratis.chronicle.constraints.IConstraint
import io.cratis.chronicle.constraints.IConstraintBuilder
import io.cratis.chronicle.events.EventType

@EventType
class ConstraintsUniqueEventTypeMessageProjectInitialized

@Constraint
class ConstraintsUniqueEventTypeMessageProjectInitialization : IConstraint {
    override fun define(builder: IConstraintBuilder) {
        builder.uniqueFor(
            ConstraintsUniqueEventTypeMessageProjectInitialized::class,
            message = "A project can only be initialized once."
        )
    }
}
```
