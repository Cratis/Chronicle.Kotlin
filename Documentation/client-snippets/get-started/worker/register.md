```kotlin
/**
 * The starter doesn't care whether the application serves HTTP or just runs in the background -
 * the same autoconfiguration connects, discovers every artifact in this application's packages,
 * and registers them with the kernel. The entry point itself needs nothing Chronicle-specific -
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
