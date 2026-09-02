```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.events.EventType;

import io.cratis.chronicle.java.EventStoreSubscriptionBuilderJavaBridge;
import io.cratis.chronicle.java.EventStoreSubscriptionsServiceJavaBridge;

@EventType
record SubscriptionsExplicitPayrollRunCompleted(String employeeId, double amount) {}

class SubscriptionsExplicitBasic {
    void subscribeToPayroll(EventStore store) {
        EventStoreSubscriptionsServiceJavaBridge.subscribe(store.getEventStoreSubscriptions(), "payroll-inbox", "PayrollEventStore", builder -> {
            EventStoreSubscriptionBuilderJavaBridge.withEventType(builder, SubscriptionsExplicitPayrollRunCompleted.class);
        });
    }
}
```
