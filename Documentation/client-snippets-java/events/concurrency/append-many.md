```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.eventSequences.AppendResult;
import io.cratis.chronicle.eventSequences.EventForEventSourceId;
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScope;
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScopeBuilder;

import java.util.List;
import java.util.Map;

import io.cratis.chronicle.java.ConcurrencyScopeBuilderJavaBridge;
import io.cratis.chronicle.java.EventSequenceJavaBridge;

@EventType
record ConcurrencyMoneyWithdrawnForTransfer(double amount) {}

@EventType
record ConcurrencyMoneyDepositedForTransfer(double amount) {}

class EventsConcurrencyAppendMany {
    // Appends to two event sources as one atomic batch, each checked against its own expected
    // sequence number.
    List<AppendResult> transferMoney(EventStore store, String fromAccount, String toAccount, double amount) {
        List<EventForEventSourceId> events = List.of(
            new EventForEventSourceId(fromAccount, new ConcurrencyMoneyWithdrawnForTransfer(amount)),
            new EventForEventSourceId(toAccount, new ConcurrencyMoneyDepositedForTransfer(amount)));

        ConcurrencyScope fromScope = ConcurrencyScopeBuilderJavaBridge
            .withSequenceNumber(new ConcurrencyScopeBuilder(), 50)
            .withEventSourceId()
            .build();
        ConcurrencyScope toScope = ConcurrencyScopeBuilderJavaBridge
            .withSequenceNumber(new ConcurrencyScopeBuilder(), 25)
            .withEventSourceId()
            .build();

        Map<String, ConcurrencyScope> concurrencyScopes = Map.of(fromAccount, fromScope, toAccount, toScope);

        return EventSequenceJavaBridge.appendMany(store.getEventLog(), events, concurrencyScopes);
    }
}
```
