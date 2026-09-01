```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor
import java.time.OffsetDateTime

@EventType
data class DecIndexUserRegistered(val name: String, val email: String, val registeredAt: OffsetDateTime)

data class DecIndexUserProfile(
    val name: String = "",
    val email: String = "",
    val registeredAt: OffsetDateTime? = null
)

class DecIndexUserProfileProjection : IProjectionFor<DecIndexUserProfile> {
    override fun define(builder: IProjectionBuilderFor<DecIndexUserProfile>) {
        builder.from(DecIndexUserRegistered::class)
    }
}
```
