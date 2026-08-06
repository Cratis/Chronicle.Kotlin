```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.eventSequences.AppendResult;
import io.cratis.chronicle.eventSequences.EventForEventSourceId;

import io.cratis.chronicle.java.EventSequenceJavaBridge;

import java.util.List;

@EventType
record TransferMoneyWithdrawn(double amount) {}

@EventType
record TransferMoneyDeposited(double amount) {}

class EventsAppendingManyTransfer {
    // Moves money between two accounts as one atomic append — each event targets its own account,
    // and either both are committed or neither of them is.
    List<AppendResult> transfer(EventStore store, String fromAccount, String toAccount, double amount) {
        return EventSequenceJavaBridge.appendMany(
            store.getEventLog(),
            List.of(
                new EventForEventSourceId(fromAccount, new TransferMoneyWithdrawn(amount)),
                new EventForEventSourceId(toAccount, new TransferMoneyDeposited(amount))));
    }
}
```
