```java title="Counters on a node"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.keys.Key;
import io.cratis.chronicle.projections.Decrement;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.Increment;
import io.cratis.chronicle.projections.graphs.Graph;
import io.cratis.chronicle.projections.graphs.Node;

import java.util.UUID;

@EventType
record TeamFormed(UUID teamId, String name) {}

@EventType
record PlayerSigned(UUID teamId) {}

@EventType
record PlayerReleased(UUID teamId) {}

@Graph("League")
@Node
@FromEvent(eventType = TeamFormed.class)
@FromEvent(eventType = PlayerSigned.class, key = "teamId")
@FromEvent(eventType = PlayerReleased.class, key = "teamId")
record Team(
    @Key UUID teamId,
    String name,
    @Increment(eventType = PlayerSigned.class) @Decrement(eventType = PlayerReleased.class) int playerCount) {}
```
