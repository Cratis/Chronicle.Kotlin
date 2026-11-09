```kotlin title="Counters on a node"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.keys.Key
import io.cratis.chronicle.projections.Decrement
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.Increment
import io.cratis.chronicle.projections.graphs.Graph
import io.cratis.chronicle.projections.graphs.Node
import java.util.UUID

@EventType
data class TeamFormed(val teamId: UUID, val name: String)

@EventType
data class PlayerSigned(val teamId: UUID)

@EventType
data class PlayerReleased(val teamId: UUID)

@Graph("League")
@Node
@FromEvent(TeamFormed::class)
@FromEvent(PlayerSigned::class, key = "teamId")
@FromEvent(PlayerReleased::class, key = "teamId")
data class Team(
    @Key val teamId: UUID,
    val name: String,
    @Increment(PlayerSigned::class) @Decrement(PlayerReleased::class) val playerCount: Int
)
```
