```kotlin
import io.cratis.chronicle.ChronicleClient
import io.cratis.chronicle.ChronicleOptions

suspend fun configureSubscriptionsAtStartup() {
    val client = ChronicleClient(ChronicleOptions.development())
    val eventStore = client.getEventStore("Quickstart")

    // Safe to call on every application startup - Subscribe is idempotent by subscription id
    eventStore.eventStoreSubscriptions.subscribe("orders-from-fulfillment", "fulfillment-service") { builder ->
        builder.withEventType(SubscriptionsExplicitShipmentDispatched::class)
    }

    eventStore.eventStoreSubscriptions.subscribe("inventory-from-warehouse", "warehouse-service") { builder ->
        builder.withEventType(SubscriptionsExplicitStockAdjusted::class)
    }
}
```
