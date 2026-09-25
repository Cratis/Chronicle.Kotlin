```java
import io.cratis.chronicle.java.BlockingChronicleClient;
import io.cratis.chronicle.ChronicleOptions;
import io.cratis.chronicle.EventStore;

import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.java.EventStoreSubscriptionBuilderJavaBridge;
import io.cratis.chronicle.java.EventStoreSubscriptionsServiceJavaBridge;

@EventType
record SubscriptionsExplicitShipmentDispatched(String shipmentId) {}

@EventType
record SubscriptionsExplicitStockAdjusted(String sku, int delta) {}

@EventType
record SubscriptionsExplicitStockReserved(String sku, int quantity) {}

class SubscriptionsExplicitTypicalPattern {
    static void registerSubscriptions(EventStore eventStore) {
        EventStoreSubscriptionsServiceJavaBridge.subscribe(eventStore.getEventStoreSubscriptions(), "orders-from-fulfillment", "fulfillment-service", builder -> {
            EventStoreSubscriptionBuilderJavaBridge.withEventType(builder, SubscriptionsExplicitShipmentDispatched.class);
        });

        EventStoreSubscriptionsServiceJavaBridge.subscribe(eventStore.getEventStoreSubscriptions(), "inventory-updates", "warehouse-service", builder -> {
            EventStoreSubscriptionBuilderJavaBridge.withEventType(builder, SubscriptionsExplicitStockAdjusted.class);
            EventStoreSubscriptionBuilderJavaBridge.withEventType(builder, SubscriptionsExplicitStockReserved.class);
        });
    }

    static void configure() {
        BlockingChronicleClient client = BlockingChronicleClient.connect(ChronicleOptions.development());
        EventStore eventStore = (EventStore) client.getEventStore("Quickstart", "Default").unwrap();
        registerSubscriptions(eventStore);
    }
}
```
