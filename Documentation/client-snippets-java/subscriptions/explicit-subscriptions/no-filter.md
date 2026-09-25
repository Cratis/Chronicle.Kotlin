```java
import io.cratis.chronicle.EventStore;

import io.cratis.chronicle.java.EventStoreSubscriptionsServiceJavaBridge;

class SubscriptionsExplicitNoFilter {
    void subscribeToEverything(EventStore store) {
        // No withEventType calls — subscribe to event types currently registered by this client.
        EventStoreSubscriptionsServiceJavaBridge.subscribe(store.getEventStoreSubscriptions(), "payroll-firehose", "PayrollEventStore", builder -> { });
    }
}
```
