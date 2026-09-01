```kotlin
/**
 * Spring's own convention is `@ComponentScan`: every class carrying a recognized stereotype in
 * the scanned packages becomes a registered bean automatically - the same mechanism that finds
 * Chronicle artifacts. `@SpringBootApplication` already implies scanning its own package and
 * below, so this is only needed to widen the scan to another package explicitly, by adding
 * `@ComponentScan("io.cratis.chronicle.samples.getstarted")` to an `@Configuration` class.
 */
object GetStartedWorkerConventionRegistration
```
