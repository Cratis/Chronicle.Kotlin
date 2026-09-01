```java
import io.cratis.chronicle.seeding.ICanSeedEvents;
import io.cratis.chronicle.seeding.IEventSeedingBuilder;
import io.cratis.chronicle.seeding.Seeder;

import io.cratis.chronicle.java.EventSeedingBuilderJavaBridge;

import java.util.List;

// Java has no equivalent of C#'s #if DEBUG - gate seed data with a runtime check instead,
// such as an environment variable or an application profile flag.
@Seeder
class EvtSeedingDevelopmentSeeding implements ICanSeedEvents {
    @Override
    public void seed(IEventSeedingBuilder builder) {
        if (!"Development".equals(System.getenv("APP_ENVIRONMENT"))) {
            return;
        }

        EventSeedingBuilderJavaBridge.forEventType(
            builder,
            EvtSeedingUserRegistered.class,
            "dev-user-1",
            List.of(new EvtSeedingUserRegistered("dev@example.com", "Dev User")));
    }
}
```
