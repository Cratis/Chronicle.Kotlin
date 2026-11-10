```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.eventSequences.EventForEventSourceId;
import io.cratis.chronicle.imports.ImportOptions;
import io.cratis.chronicle.imports.OnInvalidEvent;
import io.cratis.chronicle.imports.ImportResult;
import io.cratis.chronicle.java.BlockingEventSequence;
import io.cratis.chronicle.java.JavaEventImport;

import java.util.List;

@EventType
record ImportingLegacyOrderPlaced(String orderNumber) {}

record ImportingLegacyOrder(String orderId, String number) {}

class EventsImportingSession {
    ImportResult run(EventStore store, List<List<ImportingLegacyOrder>> batches) {
        var eventLog = new BlockingEventSequence(store.getEventLog());
        JavaEventImport importSession = eventLog.beginImport(new ImportOptions(OnInvalidEvent.Reject, true, 4)).join();
        System.out.println("Importing as " + importSession.getImportId());
        try {
            for (var batch : batches) {
                importSession.add(batch.stream()
                    .map(order -> new EventForEventSourceId(order.orderId(), new ImportingLegacyOrderPlaced(order.number())))
                    .toList()).join();
            }
            return importSession.complete().join();
        } finally {
            importSession.close().join();
        }
    }
}
```
