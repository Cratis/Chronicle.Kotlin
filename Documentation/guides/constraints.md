# Constraints

Constraints enforce business invariants at the event store level. When an
event would violate a constraint, `append` returns a failed `AppendResult`
instead of appending the event.

## Unique event type constraint

Use `uniqueFor` to prevent two event sources from ever emitting the same event type:

```kotlin
@Constraint
class OneHirePerEmployee : IConstraint {
    override fun define(builder: IConstraintBuilder) {
        builder.uniqueFor(EmployeeHired::class, "An employee can only be hired once")
    }
}
```

This makes it impossible to append a second `EmployeeHired` event for the
same event source.

## Unique property constraint

Use `unique` to prevent two event sources from sharing the same value in a
specific event field:

```kotlin
@Constraint
class UniqueEmployeeEmail : IConstraint {
    override fun define(builder: IConstraintBuilder) {
        builder.unique { unique ->
            unique
                .on(EmployeeEmailSet::class) { it.email }
                .ignoreCasing()
                .withMessage("Email address is already in use")
        }
    }
}
```

The second `EmployeeEmailSet` with an already-used email address will be rejected.

## Register constraints

Constraints must be registered before the first event is appended:

```kotlin
store.constraints.register(
    OneHirePerEmployee(),
    UniqueEmployeeEmail()
)
```

## Handling violations

```kotlin
val result = store.eventLog.append("emp-001", EmployeeHired(email = "jane@example.com"))
if (!result.isSuccess) {
    result.constraintViolations.forEach {
        println("Rejected: ${it.message}")
    }
}
```
