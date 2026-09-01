```java
import io.cratis.chronicle.ChronicleClient;
import io.cratis.chronicle.ChronicleOptions;
import io.cratis.chronicle.EventStore;

import io.cratis.chronicle.java.EventStoreSubscriptionBuilderJavaBridge;
import io.cratis.chronicle.java.EventStoreSubscriptionsServiceJavaBridge;

class SubscriptionsExplicitStartupRegistration {
    static void configure() {
        ChronicleClient client = new ChronicleClient(ChronicleOptions.Companion.development());
        EventStore eventStore = (EventStore) client.getEventStore("Quickstart", "Default");

        // Safe to call on every application startup - subscribe is idempotent by subscription id
        EventStoreSubscriptionsServiceJavaBridge.subscribe(eventStore.getEventStoreSubscriptions(), "orders-from-fulfillment", "fulfillment-service", builder -> {
            EventStoreSubscriptionBuilderJavaBridge.withEventType(builder, SubscriptionsExplicitShipmentDispatched.class);
            return null; // Java lambda returning Unit
        });

        EventStoreSubscriptionsServiceJavaBridge.subscribe(eventStore.getEventStoreSubscriptions(), "inventory-from-warehouse", "warehouse-service", builder -> {
            EventStoreSubscriptionBuilderJavaBridge.withEventType(builder, SubscriptionsExplicitStockAdjusted.class);
            return null; // Java lambda returning Unit
        });
    }
}
```
