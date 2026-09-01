```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.eventSequences.AppendResult;
import io.cratis.chronicle.eventSequences.EventForEventSourceId;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.java.BlockingEventStore;

import java.util.List;

@EventType(id = "TaggedMoneyWithdrawn")
record TaggedMoneyWithdrawn(double amount) {}

@EventType(id = "TaggedMoneyDeposited")
record TaggedMoneyDeposited(double amount) {}

class TaggedTransferService {
    private final BlockingEventStore eventStore;

    TaggedTransferService(IEventStore eventStore) {
        this.eventStore = new BlockingEventStore(eventStore);
    }

    List<AppendResult> transfer(String fromAccount, String toAccount, double amount) {
        var events = List.of(
            new EventForEventSourceId(fromAccount, new TaggedMoneyWithdrawn(amount), null, null, null, List.of("transfer", "audit")),
            new EventForEventSourceId(toAccount, new TaggedMoneyDeposited(amount), null, null, null, List.of("transfer", "audit")));

        return eventStore.getEventLog().appendMany(events);
    }
}
```
