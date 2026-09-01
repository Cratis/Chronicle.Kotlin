```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.java.BlockingChronicleClient;
import io.cratis.chronicle.namespaces.IEventStoreNamespaceResolver;

@EventType
record NamespacesItemAddedToCart(String productId, int quantity) {}

// Java has no built-in web application builder - the same shape applies no matter which web
// framework sits in front of it: resolve the namespace for the current request, ask the client
// for that event store, and append. This handler works whichever way the resolver above actually
// reads the request (a Spring Boot filter, or a plain servlet).
class Cart {
    private final BlockingChronicleClient client;
    private final IEventStoreNamespaceResolver namespaceResolver;

    public Cart(BlockingChronicleClient client, IEventStoreNamespaceResolver namespaceResolver) {
        this.client = client;
        this.namespaceResolver = namespaceResolver;
    }

    public void addItem(String cartId, String productId, int quantity) {
        client.getEventStore("production-store", namespaceResolver.resolve())
            .getEventLog()
            .append(cartId, new NamespacesItemAddedToCart(productId, quantity));
    }
}
```
