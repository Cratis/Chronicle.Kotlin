```kotlin
import io.cratis.chronicle.constraints.IConstraint
import io.cratis.chronicle.constraints.IConstraintBuilder
import io.cratis.chronicle.events.EventType

@EventType(id = "constraints-unique-event-type-project-initialized")
class ConstraintsUniqueEventTypeProjectInitialized

class ConstraintsUniqueEventTypeProjectInitialization : IConstraint {
    override fun define(builder: IConstraintBuilder) {
        builder.uniqueFor(ConstraintsUniqueEventTypeProjectInitialized::class)
    }
}
```
