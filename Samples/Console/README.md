# Chronicle Kotlin Console Sample

A runnable sample demonstrating the Chronicle Kotlin client.

## What it does

1. Appends domain events (`EmployeeHired`, `EmployeeEmailSet`, `EmployeePromoted`, `EmployeeAddressSet`, `EmployeeMoved`) to a Chronicle event store
2. Reacts to those events via `HrNotificationReactor` (logs notifications)
3. Demonstrates reducer artifact discovery (`EmployeeStateReducer`) folding events into `EmployeeState`
4. Registers a declarative projection (`EmployeeListProjection`) for the `Employee` read model
5. Queries a reducer-backed read model via `eventStore.readModels.getInstanceByKey(...)`
6. Registers a discoverable `@Seeder` artifact (`EmployeeSeeder`) and seeds initial employee events
7. Registers two discoverable `@Constraint` artifacts: `UniqueEmployeeHire` (unique-event-type constraint) and `UniqueEmployeeEmail` (unique property constraint with an index collection)
8. Demonstrates Unit of Work transactions with `eventLog.transactional` and `unitOfWorkManager.begin()`
9. Demonstrates compliance features via the `@Pii` decorator and the `Customer` read model

## Keyboard controls

Select an employee with `1`–`3`, then:

| Key | Action |
| --- | --- |
| `P` | Promote the selected employee to a new title |
| `A` | Move the selected employee to a new address |
| `E` | Set the selected employee's own (unique) email address |
| `U` | Attempt to take the next employee's email — rejected by the `UniqueEmployeeEmail` constraint |
| `R` | Read the selected employee's read-model state |
| `T` | Commit a transactional (Unit of Work) batch of events |
| `C` | Register a customer with PII-tagged events |
| `V` | View the customer PII read model |
| `I` | Switch user (cycle: Alice Smith → Bob Jones → System) |
| `H` or `?` | Show the keyboard menu |
| `Q` | Quit |

## Prerequisites

- JDK 17+
- Docker (for Chronicle Kernel)
- Gradle 8.13+

## Running

### 1. Start Chronicle

The easiest way to run Chronicle locally is via Docker:

```bash
docker run -d -p 35000:35000 -p 8080:8080 cratis/chronicle:latest-development
```

Or using the included docker-compose file in the repository root:

```bash
docker compose up -d
```

### 2. Run the sample

Use the convenience script in `Samples/Console/`:

```bash
./Samples/Console/run-sample.sh
```

Or run directly with Gradle from the repository root:

```bash
gradle :Samples:Console:run
```

You should see output with:

- A Chronicle connection log
- Event appends for hire, promotion, and relocation
- Read-model lookups for the selected employee (`R` keyboard command)
- Transactional staged appends committed as one unit (`T` keyboard command)
- Seeder status output for initial employees
- Reactor logs for observed events
- Compliance feature information (`C` and `V` keyboard commands)

### 3. Override the connection string

```bash
CHRONICLE_CONNECTION="chronicle://myserver:35000" gradle :Samples:Console:run
```

## Project structure

```
Samples/Console/src/main/kotlin/io/cratis/chronicle/samples/console/
  Main.kt                   # Interactive console entry point
  Employees.kt              # Shared employee data and helpers
  Events.kt                 # Event type declarations
  EmployeeState.kt          # EmployeeState read model
  EmployeeStateReducer.kt   # Reducer folding events into EmployeeState
  Employee.kt               # Employee read model for projections
  EmployeeListProjection.kt # Declarative projection artifact
  HrNotificationReactor.kt  # Event-driven side effects
  EmployeeSeeder.kt         # Event seeding artifact (@Seeder)
  Constraints.kt            # Discoverable @Constraint artifacts
  Compliance.kt             # PII compliance demonstration
```
