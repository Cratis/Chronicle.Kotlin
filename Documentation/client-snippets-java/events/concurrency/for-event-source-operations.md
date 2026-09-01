```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.eventSequences.AppendResult;
import io.cratis.chronicle.eventSequences.operations.EventSequenceOperations;

import java.util.List;

import io.cratis.chronicle.java.ConcurrencyScopeBuilderJavaBridge;
import io.cratis.chronicle.java.EventSequenceOperationsJavaBridge;
import io.cratis.chronicle.java.EventSourceOperationsJavaBridge;

@EventType
record ConcurrencyAccountValidated() {}

@EventType
record ConcurrencyAccountProcessed() {}

class EventsConcurrencyForEventSourceOperations {
    // Composes two events against the same event source, with a shared concurrency scope.
    List<AppendResult> processAccountBatch(EventStore store, String accountId) {
        EventSequenceOperations operations = EventSequenceOperationsJavaBridge.operationsFor(store.getEventLog());

        EventSequenceOperationsJavaBridge.forEventSourceId(operations, accountId, source -> {
            EventSourceOperationsJavaBridge.withConcurrencyScope(
                source,
                scope -> ConcurrencyScopeBuilderJavaBridge.withSequenceNumber(scope, 30).withEventSourceId());
            EventSourceOperationsJavaBridge.append(source, new ConcurrencyAccountValidated());
            EventSourceOperationsJavaBridge.append(source, new ConcurrencyAccountProcessed());
        });

        return EventSequenceOperationsJavaBridge.perform(operations);
    }
}
```
