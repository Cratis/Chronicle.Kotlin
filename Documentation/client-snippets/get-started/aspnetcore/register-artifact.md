```kotlin
/**
 * Automatic discovery registers the `@Reactor`; this factory only supplies its instance.
 * Manual registration is for applications with `cratis.chronicle.auto-discover-and-register=false`. In a Spring Boot application this factory function is exposed as an `@Bean` inside an
 * `@Configuration` class, so the constructed instance is also available for injection elsewhere.
 */
fun registerBookReturnedNotifier(): GetStartedBookReturnedNotifier {
    val notifier = GetStartedBookReturnedNotifier()
    return notifier
}
```
