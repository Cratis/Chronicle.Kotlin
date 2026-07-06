```kotlin
import io.cratis.chronicle.constraints.IConstraint
import io.cratis.chronicle.constraints.IConstraintBuilder
import io.cratis.chronicle.events.EventType

@EventType(id = "constraints-unique-casing-user-registered")
data class ConstraintsUniqueCasingUserRegistered(val email: String)

class ConstraintsUniqueCasingEmail : IConstraint {
    override fun define(builder: IConstraintBuilder) {
        builder.unique { unique ->
            unique
                .on(ConstraintsUniqueCasingUserRegistered::class) { it.email }
                .ignoreCasing()
        }
    }
}
```
