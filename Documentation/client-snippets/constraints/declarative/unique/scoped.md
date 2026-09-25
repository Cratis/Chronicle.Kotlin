```kotlin
import io.cratis.chronicle.constraints.Constraint
import io.cratis.chronicle.constraints.IConstraint
import io.cratis.chronicle.constraints.IConstraintBuilder
import io.cratis.chronicle.events.EventType

@EventType
data class ConstraintsUniqueScopedUserRegistered(val userId: String = "", val email: String = "")

/**
 * Scopes uniqueness checking to be per event source type rather than globally across the whole
 * event store. [IConstraintBuilder.perEventSourceType], [IConstraintBuilder.perEventStreamType],
 * and [IConstraintBuilder.perEventStreamId] each narrow a different dimension; combine them for
 * multiple dimensions at once.
 */
@Constraint
class ConstraintsUniqueScopedEmail : IConstraint {
    override fun define(builder: IConstraintBuilder) {
        builder
            .perEventSourceType()
            .unique { unique ->
                unique
                    .on(ConstraintsUniqueScopedUserRegistered::class, ConstraintsUniqueScopedUserRegistered::email)
                    .ignoreCasing()
                    .withMessage("Email must be unique per event source type.")
            }
    }
}
```
