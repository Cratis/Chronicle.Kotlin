```java
import io.cratis.chronicle.java.BlockingChronicleClient;
import io.cratis.chronicle.ChronicleOptions;
import io.cratis.chronicle.EventStore;

import io.cratis.chronicle.java.EventStoreSubscriptionBuilderJavaBridge;
import io.cratis.chronicle.java.EventStoreSubscriptionsServiceJavaBridge;

class SubscriptionsExplicitStartupRegistration {
    static void configure() {
        BlockingChronicleClient client = BlockingChronicleClient.connect(ChronicleOptions.development());
        EventStore eventStore = (EventStore) client.getEventStore("Quickstart", "Default").unwrap();

        // Safe to call on every application startup - subscribe is idempotent by subscription id
        EventStoreSubscriptionsServiceJavaBridge.subscribe(eventStore.getEventStoreSubscriptions(), "orders-from-fulfillment", "fulfillment-service", builder -> {
            EventStoreSubscriptionBuilderJavaBridge.withEventType(builder, SubscriptionsExplicitShipmentDispatched.class);
        });

        EventStoreSubscriptionsServiceJavaBridge.subscribe(eventStore.getEventStoreSubscriptions(), "inventory-from-warehouse", "warehouse-service", builder -> {
            EventStoreSubscriptionBuilderJavaBridge.withEventType(builder, SubscriptionsExplicitStockAdjusted.class);
        });
    }
}
```
