---
title: Constraints
description: Where the shared guide to constraints lives, with Kotlin and Java tabs.
sharedTopicBridge: true
---

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

## Kotlin client: unique over several properties

Pass more than one property to `on` when the value that must be unique is
the combination of them. Here a person is rejected only when both the first
and the last name match an existing one; sharing just one of them is fine.
`ignoreCasing()` applies to every property in the constraint:

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.constraints.Constraint
import io.cratis.chronicle.constraints.IConstraint
import io.cratis.chronicle.constraints.IConstraintBuilder
import io.cratis.chronicle.events.EventType

@EventType
data class ConstraintsBridgePersonRegistered(
    val firstName: String = "",
    val lastName: String = ""
)

@Constraint
class ConstraintsBridgeUniqueFullName : IConstraint {
    override fun define(builder: IConstraintBuilder) {
        builder.unique { unique ->
            unique
                .on(
                    ConstraintsBridgePersonRegistered::class,
                    ConstraintsBridgePersonRegistered::firstName,
                    ConstraintsBridgePersonRegistered::lastName
                )
                .ignoreCasing()
        }
    }
}
```

Call `on` once per event type, each with its own properties, to make one
constraint span several event types. Calling `on` a second time for the same
event type throws `EventTypeAlreadyAddedToUniqueConstraint` - list every
property in one call - and a `unique { }` block that never calls `on` throws
`NoEventTypesAddedToUniqueConstraint`.

When a constraint spans several event types that each have several properties,
every type must list the same number of properties in the same order, because
the kernel combines the values in declaration order.

:::caution[Earlier versions kept only the last `on` call]
Before this was supported, calling `on` more than once in a `unique { }` block
silently replaced the earlier call, so only the last event type and property
were constrained. Calls for different event types now all take effect, and a
repeated call for the same event type throws. If a constraint was relying on the
old behavior, check that it still covers what you meant it to.
:::

## Java client: unique over several properties

Java names the properties as strings through `UniqueConstraintBuilderJavaBridge`;
pass as many as make up the unique value:

<!-- validate: declarations -->

```java
import io.cratis.chronicle.constraints.Constraint;
import io.cratis.chronicle.constraints.IConstraint;
import io.cratis.chronicle.constraints.IConstraintBuilder;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.java.UniqueConstraintBuilderJavaBridge;

@EventType
record ConstraintsBridgePersonRegistered(String firstName, String lastName) {}

@Constraint
class ConstraintsBridgeUniqueFullName implements IConstraint {
    @Override
    public void define(IConstraintBuilder builder) {
        builder.unique(unique -> {
            UniqueConstraintBuilderJavaBridge
                .on(unique, ConstraintsBridgePersonRegistered.class,
                    "firstName", "lastName")
                .ignoreCasing();
        });
    }
}
```
