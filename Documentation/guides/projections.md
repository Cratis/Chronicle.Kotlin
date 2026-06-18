# Projections

A projection maps event fields to read model fields declaratively. Unlike a
reducer, you don't write fold logic — you describe which event property maps
to which read model property.

## Define the projection

Implement `IProjectionFor<T>` and annotate the class with `@Projection`:

```kotlin
@Projection
class EmployeeProjection : IProjectionFor<Employee> {
    override fun define(builder: IProjectionBuilderFor<Employee>) {
        builder
            .from(EmployeeHired::class) { from ->
                from
                    .set(Employee::id).toEventSourceId()
                    .set(Employee::firstName).toProperty("firstName")
                    .set(Employee::lastName).toProperty("lastName")
                    .set(Employee::department).toProperty("department")
            }
            .from(EmployeePromoted::class) { from ->
                from.set(Employee::title).toProperty("newTitle")
            }
    }
}
```

### Key mapping methods

| Method | Maps the read model property to... |
| --- | --- |
| `.toProperty("eventProp")` | The named field on the event |
| `.toEventSourceId()` | The event source identifier |

## Register

```kotlin
store.projections.register(EmployeeProjection())
store.readModels.register(Employee::class)
```

## Annotation-based projections

For simple, direct field mappings, annotate the read model fields instead of
implementing `IProjectionFor`:

```kotlin
@ReadModel
data class Employee(
    @SetFrom("EmployeeHired", "firstName") val firstName: String = "",
    @SetFrom("EmployeeHired", "lastName")  val lastName: String = ""
)
```

This style is more concise but less flexible than the builder-based approach
— it cannot express conditional logic or multiple sources for the same field.
