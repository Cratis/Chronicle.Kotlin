# Chronicle Java Console Sample

A runnable sample demonstrating the Chronicle Kotlin client from Java.

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

Two families of run scripts are provided:

- **`run.sh`** — all-in-one: starts Docker, waits for Chronicle, runs the sample, and stops Docker on exit. Accepts `--database` to select the backend.
- **`run-<database>.sh`** — database-specific shortcuts that delegate to `run-sample.sh`. Pass `--docker` to also manage Docker automatically, or omit it when Chronicle is already running.
- **`run-sample.sh`** — low-level launcher; accepts both `--database` and `--docker`.

### Quick start (MongoDB — fully automatic)

```bash
./Samples/Java/Console/run.sh
```

This starts Chronicle + MongoDB via docker compose, waits for it to be ready, runs the sample, and stops everything on exit.

### Select a different database

```bash
./Samples/Java/Console/run.sh --database postgresql
./Samples/Java/Console/run.sh --database mssql
./Samples/Java/Console/run.sh --database sqlite
```

### Database-specific shortcut scripts

Each `run-<database>.sh` delegates to `run-sample.sh`:

| Script | Database | Sink type | Docker flag |
| --- | --- | --- | --- |
| `run-mongodb.sh` | MongoDB (default) | `MongoDB` | `--docker` |
| `run-postgresql.sh` | PostgreSQL | `SQL` | `--docker` |
| `run-mssql.sh` | SQL Server | `SQL` | `--docker` |
| `run-sqlite.sh` | SQLite | `SQL` | `--docker` |

```bash
# Start docker + run sample (batteries included)
./Samples/Java/Console/run-mongodb.sh --docker
./Samples/Java/Console/run-postgresql.sh --docker

# Chronicle already running — skip docker
./Samples/Java/Console/run-mongodb.sh
./Samples/Java/Console/run-sample.sh --database postgresql
```

### Override the connection string

```bash
CHRONICLE_CONNECTION="chronicle://myserver:35000" ./Samples/Java/Console/run.sh
```

### Override the sink type

```bash
CHRONICLE_SINK_TYPE=SQL ./Samples/Java/Console/run.sh --database mongodb
```

## Project structure

```text
Samples/Java/Console/src/main/java/io/cratis/chronicle/samples/console/
  Main.java                   # Interactive console entry point
  Employees.java              # Shared employee data and helpers
  EmployeeHired.java          # Event type declarations
  EmployeeAddressSet.java     # Event type declarations
  EmployeePromoted.java       # Event type declarations
  EmployeeEmailSet.java       # Event type declarations
  EmployeeMoved.java          # Event type declarations
  EmployeeState.java          # EmployeeState read model
  EmployeeStateReducer.java   # Reducer folding events into EmployeeState
  Employee.java               # Employee read model for projections
  EmployeeDetails.java        # Model-bound projection read model
  EmployeeListProjection.java # Declarative projection artifact
  HrNotificationReactor.java  # Event-driven side effects
  EmployeeSeeder.java         # Event seeding artifact (@Seeder)
  Constraints.java            # Discoverable @Constraint artifacts
  Compliance.java             # PII compliance demonstration
  CustomerReducer.java        # Customer reducer
```
