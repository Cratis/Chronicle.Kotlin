```kotlin
import io.cratis.chronicle.ChronicleClient
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.namespaces.IEventStoreNamespaceResolver

@EventType
data class NamespacesItemAddedToCart(val productId: String = "", val quantity: Int = 0)

/**
 * Kotlin has no built-in web application builder - the same shape applies no matter which web
 * framework sits in front of it: resolve the namespace for the current request, ask the client for
 * that event store, and append. This handler works whichever way the resolver above actually reads
 * the request (a Spring Boot filter, Ktor, or a plain servlet).
 */
class Cart(private val client: ChronicleClient, private val namespaceResolver: IEventStoreNamespaceResolver) {
    suspend fun addItem(cartId: String, productId: String, quantity: Int) {
        val eventStore = client.getEventStore("production-store", namespaceResolver.resolve())
        eventStore.eventLog.append(cartId, NamespacesItemAddedToCart(productId, quantity))
    }
}
```
