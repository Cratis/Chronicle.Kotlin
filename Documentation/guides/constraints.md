---
sharedTopicBridge: true
---

# Constraints

Constraints are shared Chronicle behavior. The shared docs cover constraint
concepts, model-bound constraints, declarative constraints, and
client-tabbed examples.

- [Constraints](/chronicle/constraints/)
- [Understanding constraints](/chronicle/understanding-constraints/)
- [Kotlin and Java client setup](../get-started/)

## Kotlin client: constraint scoping

`IConstraintBuilder` (passed into `IConstraint.define`) can scope every
constraint added through it to a narrower uniqueness dimension than the
whole event store:

| Member | Scopes uniqueness checking per... |
| --- | --- |
| `perEventSourceType` | Event source type |
| `perEventStreamType` | Event stream type |
| `perEventStreamId` | Event stream id |

Combine any of the three; by default (none called) a constraint is checked
globally across the whole event store:

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.constraints.Constraint
import io.cratis.chronicle.constraints.IConstraint
import io.cratis.chronicle.constraints.IConstraintBuilder
import io.cratis.chronicle.events.EventType

@EventType
data class ConstraintsBridgeUserRegistered(
    val userId: String = "",
    val email: String = ""
)

@Constraint
class ConstraintsBridgeUniqueEmail : IConstraint {
    override fun define(builder: IConstraintBuilder) {
        builder
            .perEventSourceType()
            .unique { unique ->
                unique
                    .on(
                        ConstraintsBridgeUserRegistered::class,
                        ConstraintsBridgeUserRegistered::email
                    )
                    .withMessage("Email must be unique per event source type.")
            }
    }
}
```
