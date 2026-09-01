```kotlin
import io.cratis.chronicle.ChronicleClient
import io.cratis.chronicle.ChronicleOptions
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.events.EventType

@EventType
data class SubscriptionsExplicitShipmentDispatched(val shipmentId: String = "")

@EventType
data class SubscriptionsExplicitStockAdjusted(val sku: String = "", val delta: Int = 0)

@EventType
data class SubscriptionsExplicitStockReserved(val sku: String = "", val quantity: Int = 0)

suspend fun registerSubscriptions(eventStore: IEventStore) {
    eventStore.eventStoreSubscriptions.subscribe("orders-from-fulfillment", "fulfillment-service") { builder ->
        builder.withEventType(SubscriptionsExplicitShipmentDispatched::class)
    }

    eventStore.eventStoreSubscriptions.subscribe("inventory-updates", "warehouse-service") { builder ->
        builder
            .withEventType(SubscriptionsExplicitStockAdjusted::class)
            .withEventType(SubscriptionsExplicitStockReserved::class)
    }
}

suspend fun configureSubscriptionsTypicalPattern() {
    val client = ChronicleClient(ChronicleOptions.development())
    val eventStore = client.getEventStore("Quickstart")
    registerSubscriptions(eventStore)
}
```
