```kotlin
import io.cratis.chronicle.seeding.ICanSeedEvents
import io.cratis.chronicle.seeding.IEventSeedingBuilder
import io.cratis.chronicle.seeding.Seeder

@Seeder
class EvtSeedingMixedTypesSeeding : ICanSeedEvents {
    override fun seed(builder: IEventSeedingBuilder) {
        builder.forEventSource(
            "user-123",
            listOf(
                EvtSeedingUserRegistered("john@example.com", "John"),
                EvtSeedingEmailVerified("john@example.com"),
                EvtSeedingProfileUpdated("John Doe")
            )
        )
    }
}
```
