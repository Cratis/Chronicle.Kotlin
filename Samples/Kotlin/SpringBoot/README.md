# Spring Boot Sample (Kotlin)

An event-sourced HTTP API in about a hundred lines, with no Chronicle setup code
anywhere. The starter connects, finds every artifact in this package, and
registers it with the kernel before the first request is served.

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
docker run -p 35000:35000 cratis/chronicle:latest-development
gradle :Samples:Kotlin:SpringBoot:bootRun
```

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

Promote them, and read it back again:

```bash
curl -X POST http://localhost:8080/api/employees/employee-1/promote \
  -H 'Content-Type: application/json' \
  -d '{"newTitle":"Principal Engineer"}'
```

Now try to hire someone else on the same email address. The constraint rejects
it, and because the whole request runs inside a unit of work, neither event is
appended:

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
events land in the `acme` namespace instead.

## See also

- [Spring Boot guide](../../../Documentation/guides/spring-boot.md)
- [Artifact Registration](../../../Documentation/guides/artifact-registration.md)
- `Samples/Java/SpringBoot` — the same application in Java
