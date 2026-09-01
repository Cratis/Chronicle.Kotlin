```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.FilterEventsByTag
import io.cratis.chronicle.observation.Reactor
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class FilteringWithReactorOrderPlaced(val customerId: String, val totalAmount: Double)

// --- Append call ---
// Carries the "premium" tag for orders that qualify
// eventStore.eventLog.append(orderId, FilteringWithReactorOrderPlaced(customerId, total), AppendOptions(tags = listOf("premium")))

// --- Projection: receives every OrderPlaced ---
@ReadModel
@FromEvent(FilteringWithReactorOrderPlaced::class)
data class FilteringWithReactorOrderSummary(
    val customerId: String = "",
    val totalAmount: Double = 0.0
)

// --- Reactor: receives only premium-tagged OrderPlaced ---
@Reactor
@FilterEventsByTag("premium")
class FilteringWithReactorPremiumOrderNotifier {
    fun placed(event: FilteringWithReactorOrderPlaced, context: EventContext) {
    }
}
```
