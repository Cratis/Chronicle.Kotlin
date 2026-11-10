```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.eventSequences.EventForEventSourceId;
import io.cratis.chronicle.imports.ImportOptions;
import io.cratis.chronicle.imports.ImportResult;
import io.cratis.chronicle.imports.OnInvalidEvent;
import io.cratis.chronicle.java.BlockingEventSequence;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@EventType
record ImportingLegacyCustomerRegistered(String name) {}

record ImportingLegacyRow(String eventId, String customerId, String name, Instant at, long position) {}

class EventsImportingFromAStream {
    // The stream is read as the import uploads it, so the source is never held in memory as a whole.
    CompletableFuture<ImportResult> run(EventStore store, Stream<ImportingLegacyRow> rows) {
        var eventLog = new BlockingEventSequence(store.getEventLog());
        var events = rows.map(row -> new EventForEventSourceId(
            row.customerId(), new ImportingLegacyCustomerRegistered(row.name()),
            null, null, null, List.of(), row.at(), null, List.of(), null, null,
            null, null, null, row.position(), row.eventId()));
        return eventLog.importEvents(
            events,
            new ImportOptions(OnInvalidEvent.Reject),
            progress -> System.out.println(progress.getState() + ": " + progress.getEventsCommitted() + " committed"));
    }
}
```
