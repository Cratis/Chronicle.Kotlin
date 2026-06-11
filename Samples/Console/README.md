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
- Docker (to start Chronicle)
- Gradle 9+

## Running

Use a database-specific convenience script from `Samples/Console/`. Each script optionally starts Chronicle and the required infrastructure via docker compose (`--docker`) before launching the sample.

| Script | Database | Sink type |
| --- | --- | --- |
| `run-sample.sh` | Configurable via `--database` (default: MongoDB) | — |
| `run-mongodb.sh` | MongoDB (default) | `MongoDB` |
| `run-postgresql.sh` | PostgreSQL | `SQL` |
| `run-mssql.sh` | SQL Server | `SQL` |
| `run-sqlite.sh` | SQLite | `SQL` |

### Quick start (MongoDB — batteries included)

```bash
./Samples/Console/run-mongodb.sh --docker
```

This starts a Chronicle + MongoDB stack via docker compose and then runs the sample.

### PostgreSQL

```bash
./Samples/Console/run-postgresql.sh --docker
```

### SQL Server

```bash
./Samples/Console/run-mssql.sh --docker
```

### SQLite

```bash
./Samples/Console/run-sqlite.sh --docker
```

### With an already-running Chronicle

If Chronicle is already running, omit `--docker`:

```bash
./Samples/Console/run-mongodb.sh
# or
./Samples/Console/run-sample.sh --database postgresql
```

### Override the connection string

```bash
CHRONICLE_CONNECTION="chronicle://myserver:35000" ./Samples/Console/run-mongodb.sh
```

### Override the sink type

```bash
CHRONICLE_SINK_TYPE=SQL ./Samples/Console/run-sample.sh
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
