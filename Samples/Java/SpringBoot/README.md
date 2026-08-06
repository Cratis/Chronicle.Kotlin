# Spring Boot Sample (Java)

The same event-sourced HTTP API as the Kotlin sample, written in Java. There is
no Chronicle setup code anywhere: the starter connects, finds every artifact in
this package, and registers it with the kernel before the first request is
served.

Java has no coroutines, so the controller injects `Chronicle` — the same event
store with the everyday operations exposed as ordinary blocking methods.

## What is in here

| File | What it shows |
| --- | --- |
| `Application.java` | A plain `@SpringBootApplication`. That is the entire setup |
| `EmployeeHired.java` and friends | Event types as Java records |
| `EmployeeState.java` | The read model those facts fold into |
| `EmployeeStateReducer.java` | The fold itself, discovered and started for you |
| `WelcomePackageReactor.java` | A reactor taking a Spring bean through its constructor |
| `UniqueEmployeeEmail.java` | A constraint the kernel enforces on append |
| `Employees.java` | A controller injecting `Chronicle` |
| `application.yml` | Two settings: the event store, and a port of its own |

## Run it

Start a kernel, then the application. It serves on port 8081 so it can run
alongside the Kotlin sample:

```bash
docker run -p 35000:35000 cratis/chronicle:latest-development
gradle :Samples:Java:SpringBoot:bootRun
```

## Try it

Hire someone:

```bash
curl -X POST http://localhost:8081/api/employees/employee-1/hire \
  -H 'Content-Type: application/json' \
  -d '{"firstName":"Ada","lastName":"Lovelace","title":"Engineer","email":"ada@cratis.io"}'
```

Read the projected state back:

```bash
curl http://localhost:8081/api/employees/employee-1
```

Promote them, and read it back again:

```bash
curl -X POST http://localhost:8081/api/employees/employee-1/promote \
  -H 'Content-Type: application/json' \
  -d '{"newTitle":"Principal Engineer"}'
```

Now try to hire someone else on the same email address. The constraint rejects
it, and because the whole request runs inside a unit of work, neither event is
appended:

```bash
curl -i -X POST http://localhost:8081/api/employees/employee-2/hire \
  -H 'Content-Type: application/json' \
  -d '{"firstName":"Grace","lastName":"Hopper","title":"Engineer","email":"ada@cratis.io"}'
```

## See also

- [Spring Boot guide](../../../Documentation/guides/spring-boot.md)
- [Artifact Registration](../../../Documentation/guides/artifact-registration.md)
- `Samples/Kotlin/SpringBoot` — the same application in Kotlin
