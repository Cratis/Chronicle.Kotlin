```kotlin title="Declarative FromAll with a dynamic dictionary key"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

@EventType
data class UserRegisteredForEventCounts(val name: String)

@EventType
data class OrderPlacedForEventCounts(val orderId: String)

data class EventTypeCountsReadModel(
    val id: String = "",
    val eventCountByType: MutableMap<String, Long> = mutableMapOf()
)

class EventTypeCountsProjection : IProjectionFor<EventTypeCountsReadModel> {
    override fun define(builder: IProjectionBuilderFor<EventTypeCountsReadModel>) {
        builder
            .fromAll { feb ->
                feb.count(EventTypeCountsReadModel::eventCountByType, "eventType.id")
            }
    }
}
```
