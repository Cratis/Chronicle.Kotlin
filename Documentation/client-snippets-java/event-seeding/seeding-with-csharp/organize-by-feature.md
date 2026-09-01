```java
import io.cratis.chronicle.seeding.ICanSeedEvents;
import io.cratis.chronicle.seeding.IEventSeedingBuilder;
import io.cratis.chronicle.seeding.Seeder;

import io.cratis.chronicle.java.EventSeedingBuilderJavaBridge;

import java.util.List;

@Seeder
class EvtSeedingUserFeatureSeeding implements ICanSeedEvents {
    @Override
    public void seed(IEventSeedingBuilder builder) {
        EventSeedingBuilderJavaBridge.forEventType(
            builder,
            EvtSeedingUserRegistered.class,
            "test-user-1",
            List.of(new EvtSeedingUserRegistered("test1@example.com", "Test User 1")));
    }
}

@Seeder
class EvtSeedingOrderFeatureSeeding implements ICanSeedEvents {
    @Override
    public void seed(IEventSeedingBuilder builder) {
        EventSeedingBuilderJavaBridge.forEventType(
            builder,
            EvtSeedingOrderPlaced.class,
            "test-order-1",
            List.of(new EvtSeedingOrderPlaced("test-user-1", 100.00)));
    }
}
```
