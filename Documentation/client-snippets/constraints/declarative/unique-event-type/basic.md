```kotlin
import io.cratis.chronicle.constraints.IConstraint
import io.cratis.chronicle.constraints.IConstraintBuilder
import io.cratis.chronicle.events.EventType

@EventType
class ConstraintsUniqueEventTypeProjectInitialized

class ConstraintsUniqueEventTypeProjectInitialization : IConstraint {
    override fun define(builder: IConstraintBuilder) {
        builder.uniqueFor(ConstraintsUniqueEventTypeProjectInitialized::class)
    }
}
```
