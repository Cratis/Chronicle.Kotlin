```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.eventSequences.AppendResult;
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScope;
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScopeBuilder;
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyViolation;

import io.cratis.chronicle.java.AppendOptionsBuilder;
import io.cratis.chronicle.java.ConcurrencyScopeBuilderJavaBridge;
import io.cratis.chronicle.java.EventLogJavaBridge;

@EventType
record ConcurrencyStockReserved(String sku, int quantity) {}

class EventsConcurrencyBasic {
    // Appends only if the event source is still at the expected sequence number — the kernel
    // rejects the append with a concurrency violation if another writer got there first.
    void reserveStockIfUnchanged(EventStore store, String sku, long expectedSequenceNumber) {
        ConcurrencyScope scope = ConcurrencyScopeBuilderJavaBridge
            .withSequenceNumber(new ConcurrencyScopeBuilder(), expectedSequenceNumber)
            .withEventSourceId()
            .build();

        AppendResult result = EventLogJavaBridge.append(
            store.getEventLog(),
            sku,
            new ConcurrencyStockReserved(sku, 1),
            new AppendOptionsBuilder().concurrencyScope(scope).build());

        ConcurrencyViolation violation = result.getConcurrencyViolation();
        if (!result.isSuccess() && violation != null) {
            System.out.println("Concurrency violation for event source: " + violation.getEventSourceId());
        }
    }
}
```
