```kotlin title="A property graph declared with annotations"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.keys.Key
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.RemovedWith
import io.cratis.chronicle.projections.SetFromContext
import io.cratis.chronicle.projections.graphs.Edge
import io.cratis.chronicle.projections.graphs.EdgeFrom
import io.cratis.chronicle.projections.graphs.EdgeRemovedWith
import io.cratis.chronicle.projections.graphs.EdgeSource
import io.cratis.chronicle.projections.graphs.EdgeTarget
import io.cratis.chronicle.projections.graphs.Graph
import io.cratis.chronicle.projections.graphs.Node
import java.time.Instant
import java.util.UUID

@EventType
data class PersonRegistered(val personId: UUID, val name: String)

@EventType
data class PersonRemoved(val personId: UUID)

@EventType
data class GroupCreated(val groupId: UUID, val name: String)

@EventType
data class MemberAdded(val personId: UUID, val groupId: UUID)

@EventType
data class MemberRemoved(val personId: UUID, val groupId: UUID)

@Graph("Organization")
@Node
@FromEvent(PersonRegistered::class)
@RemovedWith(PersonRemoved::class, detach = true)
data class Person(@Key val personId: UUID, val name: String)

@Graph("Organization")
@Node
@FromEvent(GroupCreated::class)
data class Group(@Key val groupId: UUID, val name: String)

@Graph("Organization")
@Edge("MEMBER_OF")
@EdgeFrom(MemberAdded::class, sourceKey = "personId", targetKey = "groupId")
@EdgeRemovedWith(MemberRemoved::class, sourceKey = "personId", targetKey = "groupId")
data class MemberOf(
    @EdgeSource(Person::class) val personId: UUID,
    @EdgeTarget(Group::class) val groupId: UUID,
    @SetFromContext(MemberAdded::class, "occurred") val since: Instant
)
```
