```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.events.EventType;

import io.cratis.chronicle.java.EventStoreSubscriptionBuilderJavaBridge;
import io.cratis.chronicle.java.EventStoreSubscriptionsServiceJavaBridge;

@EventType
record SubscriptionsExplicitFilteringPayrollRunCompleted(String employeeId) {}

@EventType
record SubscriptionsExplicitFilteringPayrollRunFailed(String employeeId, String reason) {}

class SubscriptionsExplicitFiltering {
    void subscribeToPayrollOutcomes(EventStore store) {
        EventStoreSubscriptionsServiceJavaBridge.subscribe(store.getEventStoreSubscriptions(), "payroll-outcomes", "PayrollEventStore", builder -> {
            EventStoreSubscriptionBuilderJavaBridge.withEventType(builder, SubscriptionsExplicitFilteringPayrollRunCompleted.class);
            EventStoreSubscriptionBuilderJavaBridge.withEventType(builder, SubscriptionsExplicitFilteringPayrollRunFailed.class);
            return null; // Java lambda returning Unit
        });
    }
}
```
