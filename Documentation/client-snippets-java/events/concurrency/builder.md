```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.eventSequences.AppendOptions;
import io.cratis.chronicle.eventSequences.AppendResult;
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScope;
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScopeBuilder;

import io.cratis.chronicle.java.AppendOptionsBuilder;
import io.cratis.chronicle.java.ConcurrencyScopeBuilderJavaBridge;
import io.cratis.chronicle.java.EventLogJavaBridge;

@EventType
record ConcurrencyMoneyDeposited(double amount) {}

class EventsConcurrencyBuilder {
    // Uses ConcurrencyScopeBuilder to fluently narrow a concurrency scope to this account's own
    // event source id and a specific event stream type.
    AppendResult processTransaction(EventStore store, String accountId, double amount) {
        ConcurrencyScope concurrencyScope = ConcurrencyScopeBuilderJavaBridge
            .withSequenceNumber(new ConcurrencyScopeBuilder(), 15)
            .withEventSourceId()
            .withEventStreamType("Transactions")
            .build();

        AppendOptions options = new AppendOptionsBuilder()
            .eventStreamType("Transactions")
            .concurrencyScope(concurrencyScope)
            .build();

        return EventLogJavaBridge.append(store.getEventLog(), accountId, new ConcurrencyMoneyDeposited(amount), options);
    }
}
```
