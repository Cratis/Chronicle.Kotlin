```java title="A property graph declared with annotations"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.keys.Key;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.RemovedWith;
import io.cratis.chronicle.projections.SetFromContext;
import io.cratis.chronicle.projections.graphs.Edge;
import io.cratis.chronicle.projections.graphs.EdgeFrom;
import io.cratis.chronicle.projections.graphs.EdgeRemovedWith;
import io.cratis.chronicle.projections.graphs.EdgeSource;
import io.cratis.chronicle.projections.graphs.EdgeTarget;
import io.cratis.chronicle.projections.graphs.Graph;
import io.cratis.chronicle.projections.graphs.Node;

import java.time.Instant;
import java.util.UUID;

@EventType
record PersonRegistered(UUID personId, String name) {}

@EventType
record PersonRemoved(UUID personId) {}

@EventType
record GroupCreated(UUID groupId, String name) {}

@EventType
record MemberAdded(UUID personId, UUID groupId) {}

@EventType
record MemberRemoved(UUID personId, UUID groupId) {}

@Graph("Organization")
@Node
@FromEvent(eventType = PersonRegistered.class)
@RemovedWith(eventType = PersonRemoved.class, detach = true)
record Person(@Key UUID personId, String name) {}

@Graph("Organization")
@Node
@FromEvent(eventType = GroupCreated.class)
record Group(@Key UUID groupId, String name) {}

@Graph("Organization")
@Edge("MEMBER_OF")
@EdgeFrom(eventType = MemberAdded.class, sourceKey = "personId", targetKey = "groupId")
@EdgeRemovedWith(eventType = MemberRemoved.class, sourceKey = "personId", targetKey = "groupId")
record MemberOf(
    @EdgeSource(Person.class) UUID personId,
    @EdgeTarget(Group.class) UUID groupId,
    @SetFromContext(eventType = MemberAdded.class, contextProperty = "occurred") Instant since) {}
```
