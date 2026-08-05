```java
import io.cratis.chronicle.EventStore;

import io.cratis.chronicle.java.EventStoreSubscriptionsServiceJavaBridge;

class SubscriptionsExplicitUnsubscribe {
    void unsubscribeFromPayroll(EventStore store) {
        EventStoreSubscriptionsServiceJavaBridge.unsubscribe(store.getEventStoreSubscriptions(), "payroll-inbox");
    }
}
```
