```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

@EventType(id = "dec-constant-key-user-registered")
data class DecConstantKeyUserRegistered(val name: String)

@EventType(id = "dec-constant-key-user-logged-in")
data class DecConstantKeyUserLoggedIn(val placeholder: Boolean = true)

@EventType(id = "dec-constant-key-user-logged-out")
data class DecConstantKeyUserLoggedOut(val placeholder: Boolean = true)

data class DecConstantKeySiteStatistics(
    val totalUsers: Int = 0,
    val activeSessions: Int = 0
)

class DecConstantKeySiteStatisticsProjection : IProjectionFor<DecConstantKeySiteStatistics> {
    override fun define(builder: IProjectionBuilderFor<DecConstantKeySiteStatistics>) {
        builder
            .from(DecConstantKeyUserRegistered::class) {
                it.usingConstantKey("site")
                it.count(DecConstantKeySiteStatistics::totalUsers)
            }
            .from(DecConstantKeyUserLoggedIn::class) {
                it.usingConstantKey("site")
                it.increment(DecConstantKeySiteStatistics::activeSessions)
            }
            .from(DecConstantKeyUserLoggedOut::class) {
                it.usingConstantKey("site")
                it.decrement(DecConstantKeySiteStatistics::activeSessions)
            }
    }
}
```
