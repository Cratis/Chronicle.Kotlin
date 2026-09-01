```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.events.EventTypeDescriptor;
import io.cratis.chronicle.eventSequences.AppendOptions;
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScope;
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScopeBuilder;

import io.cratis.chronicle.java.ConcurrencyScopeBuilderJavaBridge;
import io.cratis.chronicle.java.EventLogJavaBridge;

@EventType
record ConcurrencyPaymentProcessed(double amount) {}

@EventType
record ConcurrencyPaymentFailed(double amount) {}

@EventType
record ConcurrencyPaymentRefunded(double amount) {}

class EventsConcurrencyEventTypes {
    /**
     * Narrows the concurrency scope to only the payment-related event types, so other event types
     * appended for the same account don't affect this check.
     */
    void processPayment(EventStore store, String accountId, double amount) {
        ConcurrencyScope concurrencyScope = ConcurrencyScopeBuilderJavaBridge
            .withSequenceNumber(new ConcurrencyScopeBuilder(), 20)
            .withEventSourceId()
            .withEventType(EventTypeDescriptor.parse("ConcurrencyPaymentProcessed"))
            .withEventType(EventTypeDescriptor.parse("ConcurrencyPaymentFailed"))
            .withEventType(EventTypeDescriptor.parse("ConcurrencyPaymentRefunded"))
            .build();

        EventLogJavaBridge.append(
            store.getEventLog(),
            accountId,
            new ConcurrencyPaymentProcessed(amount),
            new AppendOptions(null, concurrencyScope));
    }
}
```
