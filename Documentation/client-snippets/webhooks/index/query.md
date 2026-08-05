```kotlin
import io.cratis.chronicle.EventStore

suspend fun listRegisteredWebhooks(store: EventStore) {
    val webhooks = store.webhooks.getAll()
    webhooks.forEach { webhook ->
        println("${webhook.identifier} -> ${webhook.target.url}")
    }
}
```
