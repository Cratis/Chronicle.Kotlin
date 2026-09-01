```kotlin
import io.cratis.chronicle.seeding.ICanSeedEvents
import io.cratis.chronicle.seeding.IEventSeedingBuilder
import io.cratis.chronicle.seeding.Seeder

@Seeder
class EvtSeedingUserSeeding : ICanSeedEvents {
    override fun seed(builder: IEventSeedingBuilder) {
        builder
            .forEventType(
                EvtSeedingUserRegistered::class,
                "user-123",
                listOf(EvtSeedingUserRegistered("john@example.com", "John"))
            )
            .forEventSource(
                "user-456",
                listOf(
                    EvtSeedingUserRegistered("jane@example.com", "Jane"),
                    EvtSeedingEmailVerified("jane@example.com")
                )
            )
    }
}
```
