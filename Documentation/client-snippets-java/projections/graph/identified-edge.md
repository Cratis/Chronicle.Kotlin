```java title="An edge with an identity"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.keys.Key;
import io.cratis.chronicle.projections.Count;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.graphs.Edge;
import io.cratis.chronicle.projections.graphs.EdgeFrom;
import io.cratis.chronicle.projections.graphs.EdgeSource;
import io.cratis.chronicle.projections.graphs.EdgeTarget;
import io.cratis.chronicle.projections.graphs.Graph;
import io.cratis.chronicle.projections.graphs.Node;

import java.util.UUID;

@EventType
record CustomerRegistered(UUID customerId) {}

@EventType
record ShopRegistered(UUID shopId) {}

@EventType
record ShopVisited(UUID customerId, UUID shopId, UUID visitId) {}

@Graph("Visits")
@Node
@FromEvent(eventType = CustomerRegistered.class)
record Customer(@Key UUID customerId) {}

@Graph("Visits")
@Node
@FromEvent(eventType = ShopRegistered.class)
record Shop(@Key UUID shopId) {}

// The visit identifies the edge, so a customer can have several VISITED edges to one shop.
@Graph("Visits")
@Edge("VISITED")
@EdgeFrom(
    eventType = ShopVisited.class,
    sourceKey = "customerId",
    targetKey = "shopId",
    identity = "visitId")
record Visited(
    @EdgeSource(Customer.class) UUID customerId,
    @EdgeTarget(Shop.class) UUID shopId,
    @Key UUID visitId,
    @Count(eventType = ShopVisited.class) int times) {}
```
