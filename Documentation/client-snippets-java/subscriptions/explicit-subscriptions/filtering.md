```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.events.EventType;

import io.cratis.chronicle.java.EventStoreSubscriptionBuilderJavaBridge;
import io.cratis.chronicle.java.EventStoreSubscriptionsServiceJavaBridge;

@EventType(id = "subscriptions-explicit-filtering-payroll-run-completed")
record SubscriptionsExplicitFilteringPayrollRunCompleted(String employeeId) {}

@EventType(id = "subscriptions-explicit-filtering-payroll-run-failed")
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
