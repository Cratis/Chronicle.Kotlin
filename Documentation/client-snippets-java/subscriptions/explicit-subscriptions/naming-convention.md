```java
import io.cratis.chronicle.EventStore;

import io.cratis.chronicle.java.EventStoreSubscriptionsServiceJavaBridge;

class SubscriptionsExplicitNamingConvention {
    void subscribeWithStableId(EventStore store) {
        // Use a stable, descriptive id — it identifies this subscription across restarts
        // and is how you target it later with unsubscribe().
        // No filter: subscribe to event types currently registered by this client.
        EventStoreSubscriptionsServiceJavaBridge.subscribe(store.getEventStoreSubscriptions(), "payroll-inbox-v1", "PayrollEventStore", builder -> { });
    }
}
```
