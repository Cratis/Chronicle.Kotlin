```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reactor
import io.cratis.chronicle.observation.Tag

@Tag("Sales")
@Tag("Inventory")
@Tag("Customer")
@Tag("Shipping")
@Reactor
class TaggingByDomainExample {
    fun on(event: TaggingByDomainOrderPlaced) { }
}

@EventType
data class TaggingByDomainOrderPlaced(val id: String = "")
```
