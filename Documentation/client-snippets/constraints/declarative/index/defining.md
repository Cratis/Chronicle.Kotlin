```kotlin
import io.cratis.chronicle.constraints.IConstraint
import io.cratis.chronicle.constraints.IConstraintBuilder
import io.cratis.chronicle.events.EventType

@EventType(id = "constraints-unique-defining-project-created")
data class ConstraintsUniqueDefiningProjectCreated(val name: String)

class ConstraintsUniqueDefiningProjectName : IConstraint {
    override fun define(builder: IConstraintBuilder) {
        builder.unique { unique ->
            unique.on(ConstraintsUniqueDefiningProjectCreated::class) { it.name }
        }
    }
}
```
