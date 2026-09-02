```java
import io.cratis.chronicle.EventStore;

import io.cratis.chronicle.java.EventStoreSubscriptionsServiceJavaBridge;

class SubscriptionsExplicitNoFilter {
    void subscribeToEverything(EventStore store) {
        // No withEventType calls — every event type from the source outbox is subscribed to.
        EventStoreSubscriptionsServiceJavaBridge.subscribe(store.getEventStoreSubscriptions(), "payroll-firehose", "PayrollEventStore", builder -> { });
    }
}
```
