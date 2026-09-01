```kotlin
import io.cratis.chronicle.seeding.ICanSeedEvents
import io.cratis.chronicle.seeding.IEventSeedingBuilder
import io.cratis.chronicle.seeding.Seeder

@Seeder
class EvtSeedingUserFeatureSeeding : ICanSeedEvents {
    override fun seed(builder: IEventSeedingBuilder) {
        builder.forEventType(
            EvtSeedingUserRegistered::class,
            "test-user-1",
            listOf(EvtSeedingUserRegistered("test1@example.com", "Test User 1"))
        )
    }
}

@Seeder
class EvtSeedingOrderFeatureSeeding : ICanSeedEvents {
    override fun seed(builder: IEventSeedingBuilder) {
        builder.forEventType(
            EvtSeedingOrderPlaced::class,
            "test-order-1",
            listOf(EvtSeedingOrderPlaced("test-user-1", 100.00))
        )
    }
}
```
