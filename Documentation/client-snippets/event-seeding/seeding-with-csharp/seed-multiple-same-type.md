```kotlin
import io.cratis.chronicle.seeding.ICanSeedEvents
import io.cratis.chronicle.seeding.IEventSeedingBuilder
import io.cratis.chronicle.seeding.Seeder

@Seeder
class EvtSeedingMultipleSameTypeSeeding : ICanSeedEvents {
    override fun seed(builder: IEventSeedingBuilder) {
        builder.forEventType(
            EvtSeedingUserRegistered::class,
            "user-123",
            listOf(
                EvtSeedingUserRegistered("john@example.com", "John"),
                EvtSeedingUserRegistered("jane@example.com", "Jane")
            )
        )
    }
}
```
