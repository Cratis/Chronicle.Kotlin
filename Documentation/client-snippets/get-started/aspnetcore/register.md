```kotlin
/**
 * The `cratis-chronicle-spring-boot-starter` on the classpath is the entire integration - there
 * is no `AddCratisChronicle`/`UseCratisChronicle` to call. Autoconfiguration connects to the
 * kernel, discovers every event type, read model, reducer, reactor and constraint in this
 * application's packages. After the application is ready, it waits up to
 * `cratis.chronicle.registration-timeout` (30 seconds by default) for the first registration pass,
 * then continues with a warning if needed. The embedded server may already accept requests; the
 * first append waits separately for that pass. The entry point needs only Spring Boot's own `@SpringBootApplication` class
 * and `runApplication<...>(*args)`. Point it at an event store with one setting in
 * application.yml:
 *
 * ```yaml
 * cratis:
 *   chronicle:
 *     event-store: Quickstart
 * ```
 */
object AspNetCoreRegistration
```
