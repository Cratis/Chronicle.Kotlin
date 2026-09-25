```kotlin
import io.cratis.chronicle.constraints.Constraint
import io.cratis.chronicle.constraints.IConstraint
import io.cratis.chronicle.constraints.IConstraintBuilder
import io.cratis.chronicle.events.EventType

@EventType
class ConstraintsUniqueEventTypeProjectInitialized

@Constraint
class ConstraintsUniqueEventTypeProjectInitialization : IConstraint {
    override fun define(builder: IConstraintBuilder) {
        builder.uniqueFor(ConstraintsUniqueEventTypeProjectInitialized::class)
    }
}
```
