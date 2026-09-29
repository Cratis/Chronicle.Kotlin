```kotlin
import io.cratis.chronicle.constraints.Constraint
import io.cratis.chronicle.constraints.IConstraint
import io.cratis.chronicle.constraints.IConstraintBuilder
import io.cratis.chronicle.events.EventType

@EventType
data class ConstraintsUniqueUserRegistered(val email: String)

@EventType
data class ConstraintsUniqueUserEmailChanged(val newEmail: String)

// The constraint is named through @Constraint(id = ...); the fluent unique builder has no
// withName() or removedWith() on the JVM.
@Constraint(id = "UniqueEmail")
class ConstraintsUniqueEmailAcrossEvents : IConstraint {
    override fun define(builder: IConstraintBuilder) {
        builder.unique { unique ->
            unique
                .on(ConstraintsUniqueUserRegistered::class, ConstraintsUniqueUserRegistered::email)
                .on(ConstraintsUniqueUserEmailChanged::class, ConstraintsUniqueUserEmailChanged::newEmail)
        }
    }
}
```
