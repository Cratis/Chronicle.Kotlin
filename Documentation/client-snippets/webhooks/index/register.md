```kotlin
import io.cratis.chronicle.EventStore
import io.cratis.chronicle.events.EventType

@EventType
data class WebhooksIndexOrderPlaced(val orderId: String)

suspend fun registerOrderWebhook(store: EventStore) {
    store.webhooks.register("order-placed-webhook", "https://hooks.example.com/orders") { builder ->
        builder
            .withEventType(WebhooksIndexOrderPlaced::class)
            .withBearerToken(System.getenv("CHRONICLE_WEBHOOK_TOKEN") ?: error("CHRONICLE_WEBHOOK_TOKEN is required"))
    }
}
```
