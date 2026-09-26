```kotlin
import io.cratis.chronicle.constraints.Constraint
import io.cratis.chronicle.constraints.IConstraint
import io.cratis.chronicle.constraints.IConstraintBuilder
import io.cratis.chronicle.events.EventType

@EventType
data class ConstraintsUniqueCasingUserRegistered(val email: String)

@Constraint
class ConstraintsUniqueCasingEmail : IConstraint {
    override fun define(builder: IConstraintBuilder) {
        builder.unique { unique ->
            unique
                .on(ConstraintsUniqueCasingUserRegistered::class, ConstraintsUniqueCasingUserRegistered::email)
                .ignoreCasing()
        }
    }
}
```
