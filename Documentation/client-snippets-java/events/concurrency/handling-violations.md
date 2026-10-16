```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.eventSequences.AppendOptions;
import io.cratis.chronicle.eventSequences.AppendResult;
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScope;
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScopeBuilder;
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyViolation;

import io.cratis.chronicle.java.AppendOptionsBuilder;
import io.cratis.chronicle.java.EventLogJavaBridge;

@EventType
record ConcurrencySafeAccountOpened(String accountName) {}

class EventsConcurrencyHandlingViolations {
    // Handles a concurrency violation reported on AppendResult.getConcurrencyViolation() - the
    // event source id it was reported for is enough to tell which account lost the race.
    boolean tryOpenAccount(EventStore store, String accountId, String accountName) {
        // Expect that no event exists yet for this account.
        ConcurrencyScope concurrencyScope = new ConcurrencyScopeBuilder()
            .withEventSourceId()
            .withExpectsNoMatchingEvent()
            .build();

        AppendOptions options = new AppendOptionsBuilder().concurrencyScope(concurrencyScope).build();
        AppendResult result = EventLogJavaBridge.append(
            store.getEventLog(), accountId, new ConcurrencySafeAccountOpened(accountName), options);

        ConcurrencyViolation violation = result.getConcurrencyViolation();
        if (violation != null) {
            System.out.println("Concurrency violation for event source " + violation.getEventSourceId());
            return false;
        }

        return result.isSuccess();
    }
}
```
