```kotlin
/**
 * The starter doesn't care whether the application serves HTTP or just runs in the background -
 * the same autoconfiguration connects, discovers every artifact in this application's packages,
 * and registers them with the kernel. After the application is ready, it waits up to
 * `cratis.chronicle.registration-timeout` (30 seconds by default) for the first registration pass,
 * then continues with a warning if needed. A web server may already accept requests at that point;
 * the first append waits separately for that pass. The entry point needs nothing Chronicle-specific -
 * only Spring Boot's own `@SpringBootApplication` class and `runApplication<...>(*args)`. Point
 * it at an event store with one setting in application.yml:
 *
 * ```yaml
 * cratis:
 *   chronicle:
 *     event-store: Quickstart
 * ```
 */
object GetStartedWorkerRegistration
```
