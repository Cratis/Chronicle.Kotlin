# Seeding

Seeding pre-populates the event log with initial events. It is idempotent — running the same seeder twice does not duplicate events.

## Define a seeder

Annotate the class with `@Seeder` and implement `ICanSeedEvents`:

```kotlin
@Seeder
class EmployeeSeeder : ICanSeedEvents {
    override fun seed(builder: IEventSeedingBuilder) {
        builder
            .forEventSource("emp-001") {
                it.event(EmployeeHired(
                    employeeId = "emp-001",
                    firstName = "Alice",
                    lastName = "Anderson",
                    department = "Engineering"
                ))
                it.event(EmployeeEmailSet(
                    employeeId = "emp-001",
                    email = "alice@example.com"
                ))
            }
            .forEventSource("emp-002") {
                it.event(EmployeeHired(
                    employeeId = "emp-002",
                    firstName = "Bob",
                    lastName = "Baker",
                    department = "Product"
                ))
            }
    }
}
```

## Run the seeder

```kotlin
store.seeding.seed(EmployeeSeeder())
```

Call this once during application startup, before processing live traffic. Chronicle skips event sources that already have events.

## When to use seeding

- Development and test environments that need realistic initial data
- Reference data that should exist in all environments (lookup tables, configuration records)
- Migration of existing data from a legacy system into an event-sourced store
