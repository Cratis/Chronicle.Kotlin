---
title: Spring Boot
description: Add the Chronicle Spring Boot starter to a Kotlin or Java application, inject IEventStore or Chronicle, and use per-request tenancy, identity, causation and units of work.
---

The Chronicle Spring Boot starter turns "wire up an event-sourced backend" into
"add a dependency". Put it on the classpath and you get a connected client,
every artifact in your application registered with the kernel at startup,
per-request tenancy, identity and units of work, and an `IEventStore` you can
inject anywhere.

It works the same from Kotlin and from Java. It targets Spring Boot 4 and the
servlet stack (Spring MVC); the per-request features do not apply to WebFlux.

## Add the dependency

The starter is published to Maven Central as
`io.cratis:chronicle-spring-boot-starter`, and brings the client with it. It is
built against Spring Boot 4.1.1. This page was checked with `6.5.0`.

<!-- validate: skip -->

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.cratis:chronicle-spring-boot-starter:6.5.0")
}
```

```groovy
// build.gradle
dependencies {
    implementation 'io.cratis:chronicle-spring-boot-starter:6.5.0'
}
```

With Maven:

```xml
<dependency>
    <groupId>io.cratis</groupId>
    <artifactId>chronicle-spring-boot-starter</artifactId>
    <version>6.5.0</version>
</dependency>
```

:::note[Upgrading from 6.4.0 or earlier]
Versions up to 6.4.0 needed `kotlinx-coroutines` 1.11, above the 1.10.2 that
Spring Boot 4.1 manages, and failed at startup with
`NoSuchMethodError: 'java.lang.Object kotlinx.coroutines.BuildersKt.runBlockingK(…)'`
unless the application set Spring Boot's `kotlin-coroutines.version` property
to `1.11.0`. From 6.5.0 the starter runs on the managed version, so you can
remove that override.
:::

## Configure it

Everything has a default that works on a developer's machine, so the only setting
most applications need is the name of their event store:

```yaml
cratis:
  chronicle:
    event-store: Ordering
```

That gives you the local development kernel on `localhost:35000`, the `Default`
namespace, and automatic discovery and registration of every artifact in your
application's packages. For any other kernel, set
`cratis.chronicle.connection-string`, with `?skipTlsValidation=false` and the
secret supplied from the environment (for example through
`CRATIS_CHRONICLE_CONNECTION_STRING`); see
[Configuration](../reference/configuration.md#tls-and-authentication).

## Write artifacts, not wiring

There is no registration code. Write the event, the read model, and the reducer,
and they are found and registered on startup:

<!-- validate: skip -->

```kotlin
@EventType
data class EmployeeHired(
    val firstName: String = "",
    val lastName: String = "",
    val title: String = ""
)

@ReadModel
data class EmployeeState(
    val id: String = "",
    val firstName: String = "",
    val title: String = ""
)

@Reducer
class EmployeeStateReducer {
    fun employeeHired(event: EmployeeHired): EmployeeState =
        EmployeeState(firstName = event.firstName, title = event.title)
}
```

Discovery is scoped to the packages Spring Boot already scans for components —
the package of your `@SpringBootApplication` class and everything beneath it.
Override it with `cratis.chronicle.artifact-packages` when your artifacts live
somewhere else.

## Artifacts are Spring components

An artifact is activated through the container, so it takes its dependencies
through its constructor exactly like a `@Service` would:

<!-- validate: skip -->

```kotlin
@Reactor
class WelcomePackageReactor(private val mailer: Mailer) {
    fun employeeHired(
        event: EmployeeHired,
        context: EventContext
    ): WelcomePackageRequested {
        val address = "${event.firstName}.${event.lastName}@example.com"
        mailer.send(address, "Welcome to the team!")
        return WelcomePackageRequested(context.eventSourceId)
    }
}
```

<!-- validate: skip -->

```java
@Reactor
public class WelcomePackageReactor {
    private final Mailer mailer;

    public WelcomePackageReactor(Mailer mailer) {
        this.mailer = mailer;
    }

    public WelcomePackageRequested employeeHired(
            EmployeeHired event, EventContext context) {
        mailer.send(event.firstName() + "." + event.lastName() + "@example.com",
            "Welcome to the team!");
        return new WelcomePackageRequested(context.getEventSourceId());
    }
}
```

Declaring an artifact as a `@Component` is optional. If you do, that bean is the
one used, with its own scope and lifecycle. If you do not, it is still
constructed with everything it needs injected.

## Using it from Kotlin

Inject `IEventStore` and use the full coroutine API. Spring MVC handlers run on
a request thread, so bridge with `runBlocking`:

<!-- validate: skip -->

```kotlin
@RestController
@RequestMapping("/api/employees")
class Employees(private val eventStore: IEventStore) {
    @PostMapping("/{id}/hire")
    fun hire(@PathVariable id: String, @RequestBody hire: Hire) = runBlocking {
        eventStore.eventLog.append(
            id, EmployeeHired(hire.firstName, hire.lastName, hire.title))
        ResponseEntity.accepted().build<Any>()
    }

    @GetMapping("/{id}")
    fun get(@PathVariable id: String) = runBlocking {
        eventStore.readModels.getInstanceByKey(EmployeeState::class, id)
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.notFound().build()
    }
}
```

Keep the `runBlocking` bridge on the request thread rather than making the
handler `suspend`. The current identity, causation chain, namespace and unit of
work are held per thread, and `runBlocking` keeps your code on the thread the
request filters set them on.

The read model is updated after the append, not during it, so a `GET` issued
right after the `POST` can still find nothing. See
[Connection lifecycle](../reference/connection-lifecycle.md#reading-after-writing).

## Using it from Java

Java has no coroutines, so inject `Chronicle` instead. It is the same event
store, with the everyday operations exposed as ordinary blocking methods:

<!-- validate: skip -->

```java
@RestController
@RequestMapping("/api/employees")
public class Employees {
    private final Chronicle chronicle;

    public Employees(Chronicle chronicle) {
        this.chronicle = chronicle;
    }

    @PostMapping("/{id}/hire")
    public ResponseEntity<Object> hire(
            @PathVariable String id, @RequestBody Hire hire) {
        chronicle.append(id,
            new EmployeeHired(hire.firstName(), hire.lastName(), hire.title()));
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{id}")
    public EmployeeState get(@PathVariable String id) {
        return chronicle.readModel(EmployeeState.class, id);
    }
}
```

`readModel` returns `null` when there is no instance yet, which includes the
moment right after the append that creates it. Anything beyond the everyday is
one hop away through `chronicle.getEventStore()`, which is the full API.

| Method on `Chronicle` | Description |
| --- | --- |
| `append(eventSourceId, event)` | Append one event |
| `appendMany(eventSourceId, events)` | Append several to one event source |
| `readModel(type, key)` | One read model instance by key |
| `readModels(type)` | Every instance of a read model |
| `readModelHistory(type, key)` | Every state an instance has been through |
| `inUnitOfWork(work)` | Run work in a unit of work, then commit it |
| `getEventStore()` | The full `IEventStore` |

## What you get per request

In a servlet application the starter adds three filters. Each can be turned off
on its own.

### Identity

The authenticated principal becomes the identity recorded on every event appended
while handling the request, so the audit trail comes out right without any code
passing a user around. Subject, name and username are read from the `sub`, `name`
and `preferred_username` claims, falling back to the principal's name.

Requires Spring Security on the classpath. Disable with
`cratis.chronicle.identity.enabled: false`.

### Causation

The route, method, host, scheme and query of the request are recorded on the
causation chain of every event it produces. "Why does this record say what it
says?" is then answered by the event's own metadata rather than by correlating
log files.

Disable with `cratis.chronicle.causation.enabled: false`.

### Unit of work

Each request runs inside a unit of work that is committed when the request
completes and rolled back if it throws. Only appends made through
`eventLog.transactional` are staged in it. A plain `eventLog.append`, and
`Chronicle.append` from Java, go to the kernel straight away and are not
undone when the request fails.

Consecutive staged events for the same event source, event sequence and append
options are appended as one atomic batch: either all of them land or none do.
Anything else starts a new batch, including a different event source, and the
batches are sent one after another. One batch can land while a later one is
rejected.

The filter commits after your handler has returned, which is too late to tell
the caller about a rejected append. When the response depends on the outcome,
commit in the handler:

<!-- validate: skip -->

```kotlin
@PostMapping("/{id}/hire")
fun hire(@PathVariable id: String, @RequestBody hire: Hire) = runBlocking {
    // Staged in the request's unit of work; nothing is sent yet.
    eventStore.eventLog.transactional.append(
        id, EmployeeHired(hire.firstName, hire.lastName, hire.title))
    eventStore.eventLog.transactional.append(id, EmployeeEmailSet(hire.email))

    // Consecutive events for the same event source with the same options go in
    // one batch. If the email is already taken, the constraint stops both.
    val unitOfWork = eventStore.unitOfWorkManager.current
    unitOfWork.commit()

    if (unitOfWork.isSuccess) {
        ResponseEntity.accepted().build<Any>()
    } else {
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(unitOfWork.getConstraintViolations().map { it.message })
    }
}
```

While a unit of work is active, `transactional.append` returns a placeholder
result with sequence number `-1` and `isSuccess` set to `true`; the real
outcome is on the unit of work after `commit()`. A handler that commits or
rolls back itself is left alone by the filter. Disable the filter with
`cratis.chronicle.unit-of-work.enabled: false`.

## Multi-tenancy

A namespace is Chronicle's tenancy boundary: the same event store, the same
artifacts, entirely separate streams of events. The injected `IEventStore` routes
to the namespace the current piece of work belongs to on every call, so
application code never mentions a tenant.

Pick how the tenant is decided:

```yaml
cratis:
  chronicle:
    namespace-resolution:
      strategy: http-header    # fixed | http-header | subdomain | authentication
      http-header: x-cratis-tenant-id
      claim: tenant_id
```

| Strategy | Where the namespace comes from |
| --- | --- |
| `fixed` | `cratis.chronicle.namespace`. The single-tenant default |
| `http-header` | A header on the current request |
| `subdomain` | The first label of the host — `acme` in `acme.example.com` |
| `authentication` | A claim on the authenticated principal |

`authentication` is the strongest of the four, because the tenant comes from the
token rather than from anything a caller can set. It still falls back to
`Default` when a request is not authenticated or the token has no tenant claim,
so require authentication and reject requests without a valid tenant claim
before they reach Chronicle. Work outside a request, such
as a scheduled job or a reactor, falls back to the `Default` namespace.

:::danger[A header or subdomain is chosen by the caller]
With `http-header` or `subdomain`, whoever sends the request picks the
namespace, and a request without the header or a subdomain lands in
`Default`. Use them for
tenant isolation only behind something that checks the caller belongs to that
tenant, or use `authentication`.
:::

Declare your own `IEventStoreNamespaceResolver` bean to decide it any other way:

<!-- validate: skip -->

```kotlin
@Bean
fun namespaceResolver(tenants: TenantDirectory) =
    IEventStoreNamespaceResolver { tenants.currentTenant().namespace }
```

## Startup

The client is created while the application context starts, and it connects
right away. If the kernel is unreachable at that moment, or incompatible, the
`chronicleClient` bean fails and the application does not start
(`APPLICATION FAILED TO START`, caused by `io.grpc.StatusException: UNAVAILABLE`).

Once the application is ready, the starter waits for the first registration
pass, for at most `cratis.chronicle.registration-timeout` (30 seconds by
default), and logs `Chronicle artifacts registered with event store '…'`. By
then the web server is already accepting requests. That is safe for appends:
the first append waits for the same registration pass. If the pass has not
finished in time, the starter logs a warning and carries on, and the client
keeps trying to connect in the background; requests that append wait until it
succeeds, so treat that warning as an outage. When the kernel rejects the
connection, because the credentials are wrong or the server is incompatible,
the wait fails with `ChronicleConnectionFailed` instead, and the application
stops with `Application run failed`. See [Connection lifecycle](../reference/connection-lifecycle.md)
for the underlying behavior.

## Configuration reference

| Property | Default | Description |
| --- | --- | --- |
| `connection-string` | development kernel | Where the kernel is |
| `event-store` | `Default` | The event store to work against |
| `namespace` | `Default` | Namespace for the `fixed` strategy |
| `auto-discover-and-register` | `true` | Register artifacts on connect |
| `artifact-packages` | application packages | Packages to scan |
| `default-sink-type-id` | `MongoDB` | Where read models are persisted |
| `program-identifier` | `spring.application.name` | Name in diagnostics |
| `registration-timeout` | `30s` | How long startup waits for registration |
| `namespace-resolution.strategy` | `fixed` | How the namespace is decided |
| `namespace-resolution.http-header` | `x-cratis-tenant-id` | Header to read |
| `namespace-resolution.claim` | `tenant_id` | Claim to read |
| `unit-of-work.enabled` | `true` | Unit of work per request |
| `causation.enabled` | `true` | Request causation on events |
| `identity.enabled` | `true` | Authenticated user as event identity |

## Replacing what the starter provides

Every bean the starter contributes backs off the moment your application declares
its own, so nothing has to be turned off before it can be replaced — declare a
`ChronicleOptions`, `IChronicleClient`, `IEventStore`, `IArtifactActivator` or
`IEventStoreNamespaceResolver` bean and yours wins.

## Samples

- `Samples/Kotlin/SpringBoot` — the Kotlin version of everything above
- `Samples/Java/SpringBoot` — the same application in Java

Both need a running kernel. From the repository root:

```bash
docker run --rm -p 127.0.0.1:35000:35000 cratis/chronicle:latest-development
./gradlew :Samples:Kotlin:SpringBoot:bootRun
```

## See also

- [Artifact Registration](artifact-registration.md): what gets discovered, and
  how to narrow, replace or turn off discovery
- [Connection lifecycle](../reference/connection-lifecycle.md): connecting,
  reconnecting, and where failures show up
