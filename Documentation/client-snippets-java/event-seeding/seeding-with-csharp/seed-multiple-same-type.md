```java
import io.cratis.chronicle.seeding.ICanSeedEvents;
import io.cratis.chronicle.seeding.IEventSeedingBuilder;
import io.cratis.chronicle.seeding.Seeder;

import io.cratis.chronicle.java.EventSeedingBuilderJavaBridge;

import java.util.List;

@Seeder
class EvtSeedingMultipleSameTypeSeeding implements ICanSeedEvents {
    @Override
    public void seed(IEventSeedingBuilder builder) {
        EventSeedingBuilderJavaBridge.forEventType(
            builder,
            EvtSeedingUserRegistered.class,
            "user-123",
            List.of(
                new EvtSeedingUserRegistered("john@example.com", "John"),
                new EvtSeedingUserRegistered("jane@example.com", "Jane")));
    }
}
```
