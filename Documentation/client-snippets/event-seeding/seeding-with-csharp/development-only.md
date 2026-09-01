```kotlin
import io.cratis.chronicle.seeding.ICanSeedEvents
import io.cratis.chronicle.seeding.IEventSeedingBuilder
import io.cratis.chronicle.seeding.Seeder

// Kotlin has no equivalent of C#'s #if DEBUG - gate seed data with a runtime check instead,
// such as an environment variable or an application profile flag.
@Seeder
class EvtSeedingDevelopmentSeeding : ICanSeedEvents {
    override fun seed(builder: IEventSeedingBuilder) {
        if (System.getenv("APP_ENVIRONMENT") != "Development") return

        builder.forEventType(
            EvtSeedingUserRegistered::class,
            "dev-user-1",
            listOf(EvtSeedingUserRegistered("dev@example.com", "Dev User"))
        )
    }
}
```
