```kotlin
/**
 * The `cratis-chronicle-spring-boot-starter` on the classpath is the entire integration - there
 * is no `AddCratisChronicle`/`UseCratisChronicle` to call. Autoconfiguration connects to the
 * kernel, discovers every event type, read model, reducer, reactor and constraint in this
 * application's packages, and registers them before the first request is served. The entry point
 * itself needs nothing Chronicle-specific - only Spring Boot's own `@SpringBootApplication` class
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
