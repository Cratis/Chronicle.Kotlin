```java
import io.cratis.chronicle.seeding.ICanSeedEvents;
import io.cratis.chronicle.seeding.IEventSeedingBuilder;
import io.cratis.chronicle.seeding.Seeder;

import java.util.List;

@Seeder
class EvtSeedingMixedTypesSeeding implements ICanSeedEvents {
    @Override
    public void seed(IEventSeedingBuilder builder) {
        builder.forEventSource(
            "user-123",
            List.of(
                new EvtSeedingUserRegistered("john@example.com", "John"),
                new EvtSeedingEmailVerified("john@example.com"),
                new EvtSeedingProfileUpdated("John Doe")));
    }
}
```
