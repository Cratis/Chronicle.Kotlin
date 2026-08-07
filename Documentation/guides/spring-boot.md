# Spring Boot

The Chronicle Spring Boot starter turns "wire up an event-sourced backend" into
"add a dependency". Put it on the classpath and you get a connected client, every
artifact in your application registered with the kernel before the first request
is served, per-request tenancy, identity and units of work — and an `IEventStore`
you can inject anywhere.

It works the same from Kotlin and from Java.

## Add the dependency

The starter is published to Maven Central as
`io.cratis:chronicle-spring-boot-starter`, and brings the client with it.

<!-- validate: skip -->

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.cratis:chronicle-spring-boot-starter:2.1.1")
}
```

```groovy
// build.gradle
dependencies {
    implementation 'io.cratis:chronicle-spring-boot-starter:2.1.1'
}
```

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
application's packages.

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
        mailer.send(event.email, "Welcome to the team!")
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
        mailer.send(event.email(), "Welcome to the team!");
        return new WelcomePackageRequested(context.getEventSourceId());
    }
}
```

Declaring an artifact as a `@Component` is optional. If you do, that bean is the
one used, with its own scope and lifecycle. If you do not, it is still
constructed with everything it needs injected.

## Using it from Kotlin

Inject `IEventStore` and use the full coroutine API. Spring MVC handlers are
blocking, so bridge with `runBlocking` — on WebFlux, mark the handler `suspend`
and drop the bridge:

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
    }
}
```

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

Anything beyond the everyday is one hop away through `chronicle.getEventStore()`,
which is the full API.

| Method on `Chronicle` | Description |
| --- | --- |
| `append(eventSourceId, event)` | Append one event |
| `appendMany(eventSourceId, events)` | Append several to one event source |
| `readModel(type, key)` | One read model instance by key |
| `readModels(type)` | Every instance of a read model |
| `readModelHistory(type, key)` | Every state an instance has been through |
| `inUnitOfWork(work)` | Run work atomically outside a request |
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
completes and rolled back if it throws. A handler can append several events
across several event sources and have them land together or not at all:

<!-- validate: skip -->

```kotlin
@PostMapping("/{id}/hire")
fun hire(@PathVariable id: String, @RequestBody hire: Hire) = runBlocking {
    // Both events commit together when the request completes. If the email is
    // already taken, the constraint stops both.
    eventStore.eventLog.append(
        id, EmployeeHired(hire.firstName, hire.lastName, hire.title))
    eventStore.eventLog.append(id, EmployeeEmailSet(hire.email))
    ResponseEntity.accepted().build<Any>()
}
```

A handler that commits or rolls back itself is left alone. Disable with
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
token rather than from anything a caller can set. Work outside a request — a
scheduled job, a reactor — falls back to the `Default` namespace.

Declare your own `IEventStoreNamespaceResolver` bean to decide it any other way:

<!-- validate: skip -->

```kotlin
@Bean
fun namespaceResolver(tenants: TenantDirectory) =
    IEventStoreNamespaceResolver { tenants.currentTenant().namespace }
```

## Startup

The starter connects on startup and holds the application back until every
artifact is registered, so the first request never reaches a kernel that has not
been told about the event types it is about to be handed.

The wait is bounded by `cratis.chronicle.registration-timeout` (30 seconds by
default). If the kernel cannot be reached in time the application starts anyway,
logs why, and keeps trying in the background — a temporarily unavailable kernel
degrades rather than blocks.

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
| `registration-timeout` | `30s` | How long startup waits for it |
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

Both need a running kernel:

```bash
docker run -p 35000:35000 cratis/chronicle:latest-development
gradle :Samples:Kotlin:SpringBoot:bootRun
```

## See also

- [Artifact Registration](artifact-registration.md) — what gets discovered, and
  how to narrow, replace or turn off discovery
