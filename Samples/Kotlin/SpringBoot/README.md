# Spring Boot Sample (Kotlin)

An event-sourced HTTP API in about a hundred lines, with no Chronicle setup code
anywhere. The starter connects, finds every artifact in this package, and
registers it with the kernel as the application starts.

## What is in here

| File | What it shows |
| --- | --- |
| `Application.kt` | A plain `@SpringBootApplication`. That is the entire setup |
| `Events.kt` | Event types — facts about what happened |
| `EmployeeState.kt` | The read model those facts fold into |
| `EmployeeStateReducer.kt` | The fold itself, discovered and started for you |
| `WelcomePackageReactor.kt` | A reactor taking a Spring bean through its constructor |
| `UniqueEmployeeEmail.kt` | A constraint the kernel enforces on append |
| `Employees.kt` | A controller injecting `IEventStore` |
| `application.yml` | One setting: the name of the event store |

## Run it

Start a kernel, then the application:

```bash
docker run --rm -p 127.0.0.1:35000:35000 cratis/chronicle:latest-development
./gradlew :Samples:Kotlin:SpringBoot:bootRun
```

Run the Gradle command from the repository root with a JDK 17 or later on
`JAVA_HOME`. The build sets Spring Boot's `kotlin-coroutines.version` to
`1.11.0`, which the client needs; your own application needs the same line (see
the [Spring Boot guide](../../../Documentation/guides/spring-boot.md#add-the-dependency)).

## Try it

Hire someone:

```bash
curl -X POST http://localhost:8080/api/employees/employee-1/hire \
  -H 'Content-Type: application/json' \
  -d '{"firstName":"Ada","lastName":"Lovelace","title":"Engineer","email":"ada@cratis.io"}'
```

Read the projected state back:

```bash
curl http://localhost:8080/api/employees/employee-1
```

The reducer runs after the append, so a `404` straight after hiring means the
read model has not caught up yet. Ask again a moment later.

Promote them, and read it back again:

```bash
curl -X POST http://localhost:8080/api/employees/employee-1/promote \
  -H 'Content-Type: application/json' \
  -d '{"newTitle":"Principal Engineer"}'
```

Now try to hire someone else on the same email address. The constraint rejects
the email and the request answers `409 Conflict`:

```bash
curl -i -X POST http://localhost:8080/api/employees/employee-2/hire \
  -H 'Content-Type: application/json' \
  -d '{"firstName":"Grace","lastName":"Hopper","title":"Engineer","email":"ada@cratis.io"}'
```

## Multi-tenancy

Switch the sample to a namespace per tenant by adding this to
`application.yml` — nothing in the application code changes:

```yaml
cratis:
  chronicle:
    namespace-resolution:
      strategy: http-header
      http-header: x-cratis-tenant-id
```

Then pass `-H 'x-cratis-tenant-id: acme'` on any of the calls above and the
events land in the `acme` namespace instead. The caller chooses that header, so
this is a demonstration of routing, not of tenant isolation.

## What this sample does not show

The controller appends with `eventStore.eventLog.append`, which goes to the
kernel straight away. The request's unit of work only stages appends made
through `eventStore.eventLog.transactional`. So when the email is taken,
`EmployeeHired` for `employee-2` has already been appended before
`EmployeeEmailSet` is rejected. The
[Spring Boot guide](../../../Documentation/guides/spring-boot.md#unit-of-work)
shows how to stage both and commit them as one batch.

## See also

- [Spring Boot guide](../../../Documentation/guides/spring-boot.md)
- [Artifact Registration](../../../Documentation/guides/artifact-registration.md)
- `Samples/Java/SpringBoot` — the same application in Java
