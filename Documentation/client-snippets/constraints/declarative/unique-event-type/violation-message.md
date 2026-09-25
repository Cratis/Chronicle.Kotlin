```kotlin
import io.cratis.chronicle.constraints.Constraint
import io.cratis.chronicle.constraints.IConstraint
import io.cratis.chronicle.constraints.IConstraintBuilder
import io.cratis.chronicle.events.EventType

@EventType
class ConstraintsUniqueEventTypeMessageProjectInitialized

@Constraint
class ConstraintsUniqueEventTypeMessageProjectInitialization : IConstraint {
    override fun define(builder: IConstraintBuilder) {
        // Not sent to the kernel by io.cratis:chronicle 6.4.0; a violation carries the kernel's own message.
        builder.uniqueFor(
            ConstraintsUniqueEventTypeMessageProjectInitialized::class,
            message = "A project can only be initialized once."
        )
    }
}
```
