```kotlin title="An edge with an identity"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.keys.Key
import io.cratis.chronicle.projections.Count
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.graphs.Edge
import io.cratis.chronicle.projections.graphs.EdgeFrom
import io.cratis.chronicle.projections.graphs.EdgeSource
import io.cratis.chronicle.projections.graphs.EdgeTarget
import io.cratis.chronicle.projections.graphs.Graph
import io.cratis.chronicle.projections.graphs.Node
import java.util.UUID

@EventType
data class CustomerRegistered(val customerId: UUID)

@EventType
data class ShopRegistered(val shopId: UUID)

@EventType
data class ShopVisited(val customerId: UUID, val shopId: UUID, val visitId: UUID)

@Graph("Visits")
@Node
@FromEvent(CustomerRegistered::class)
data class Customer(@Key val customerId: UUID)

@Graph("Visits")
@Node
@FromEvent(ShopRegistered::class)
data class Shop(@Key val shopId: UUID)

// The visit identifies the edge, so a customer can have several VISITED edges to one shop.
@Graph("Visits")
@Edge("VISITED")
@EdgeFrom(
    ShopVisited::class,
    sourceKey = "customerId",
    targetKey = "shopId",
    identity = "visitId"
)
data class Visited(
    @EdgeSource(Customer::class) val customerId: UUID,
    @EdgeTarget(Shop::class) val shopId: UUID,
    @Key val visitId: UUID,
    @Count(ShopVisited::class) val times: Int
)
```
