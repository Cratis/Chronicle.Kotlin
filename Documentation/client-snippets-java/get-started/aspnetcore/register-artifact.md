```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.java.ReactorsServiceJavaBridge;

// Autodiscovery already finds every @Reactor in a scanned package - this is only needed when a
// reactor is constructed with something Spring cannot inject on its own, or when
// cratis.chronicle.auto-discover-and-register is turned off and every artifact is registered by
// hand. In a Spring Boot application this factory method is exposed as an @Bean inside an
// @Configuration class, so the constructed instance is also available for injection elsewhere.
class AspNetCoreExplicitRegistration {
    GetStartedBookReturnedNotifier bookReturnedNotifier(IEventStore eventStore) {
        GetStartedBookReturnedNotifier notifier = new GetStartedBookReturnedNotifier();
        ReactorsServiceJavaBridge.register(eventStore.getReactors(), notifier);
        return notifier;
    }
}
```
