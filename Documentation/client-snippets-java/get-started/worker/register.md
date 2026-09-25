```java
/**
 * The starter doesn't care whether the application serves HTTP or just runs in the background -
 * the same autoconfiguration connects, discovers every artifact in this application's packages,
 * and registers them with the kernel. After the application is ready, it waits up to
 * {@code cratis.chronicle.registration-timeout} (30 seconds by default) for the first registration
 * pass, then continues with a warning if needed. A web server may already accept requests;
 * the first append waits separately for that pass. The entry point needs nothing Chronicle-specific -
 * only Spring Boot's own {@code @SpringBootApplication} class and
 * {@code SpringApplication.run(...)}. Point it at an event store with one setting in
 * application.yml:
 *
 * <pre>
 * cratis:
 *   chronicle:
 *     event-store: Quickstart
 * </pre>
 */
class GetStartedWorkerRegistration {
}
```
